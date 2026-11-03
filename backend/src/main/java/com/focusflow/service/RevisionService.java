package com.focusflow.service;

import com.focusflow.dto.*;
import com.focusflow.entity.*;
import com.focusflow.exception.BadRequestException;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RevisionService implements the SM-2 Spaced Repetition Engine for FocusFlow.
 * Handles automatic revision scheduling after topic completion, recall evaluation,
 * adaptive interval and easiness factor updates, overdue detection, and flashcards.
 */
@Service
@Transactional
public class RevisionService {

    private static final Logger logger = LoggerFactory.getLogger(RevisionService.class);

    private final RevisionSessionRepository revisionSessionRepository;
    private final RevisionHistoryRepository revisionHistoryRepository;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardReviewRepository flashcardReviewRepository;
    private final TopicRepository topicRepository;

    public RevisionService(RevisionSessionRepository revisionSessionRepository,
                           RevisionHistoryRepository revisionHistoryRepository,
                           FlashcardRepository flashcardRepository,
                           FlashcardReviewRepository flashcardReviewRepository,
                           TopicRepository topicRepository) {
        this.revisionSessionRepository = revisionSessionRepository;
        this.revisionHistoryRepository = revisionHistoryRepository;
        this.flashcardRepository = flashcardRepository;
        this.flashcardReviewRepository = flashcardReviewRepository;
        this.topicRepository = topicRepository;
    }

    /**
     * Creates an initial spaced revision session for a completed topic.
     * Prevents duplicate pending/overdue revisions for the same topic.
     */
    public RevisionSessionDto createInitialRevision(Topic topic) {
        if (topic == null || topic.getId() == null) {
            throw new BadRequestException("Valid topic is required to create a revision session");
        }

        // Check if an active revision already exists for this topic
        Optional<RevisionSession> existing = revisionSessionRepository
                .findFirstByTopicIdAndStatusInOrderByScheduledDateDesc(
                        topic.getId(),
                        List.of(RevisionStatus.PENDING, RevisionStatus.OVERDUE)
                );

        if (existing.isPresent()) {
            logger.info("Active revision already exists for topic '{}' (ID {}). Skipping duplicate.",
                    topic.getName(), topic.getId());
            return mapToDto(existing.get());
        }

        // First revision: 1 day after completion
        LocalDate initialDate = LocalDate.now().plusDays(1);
        RevisionSession session = new RevisionSession(
                topic,
                initialDate,
                0, // initial repetition count
                1, // 1 day interval
                2.5, // initial easiness factor
                RevisionStatus.PENDING
        );

        RevisionSession saved = revisionSessionRepository.save(session);
        logger.info("Created initial revision session for topic '{}' scheduled for {}",
                topic.getName(), initialDate);
        return mapToDto(saved);
    }

    /**
     * Retrieves revision tasks due today or overdue.
     */
    public List<RevisionSessionDto> getTodayRevisions() {
        LocalDate today = LocalDate.now();
        List<RevisionSession> dueSessions = revisionSessionRepository
                .findByScheduledDateLessThanEqualAndStatusInOrderByScheduledDateAsc(
                        today,
                        List.of(RevisionStatus.PENDING, RevisionStatus.OVERDUE)
                );

        // Update overdue status if date is strictly in the past
        for (RevisionSession session : dueSessions) {
            if (session.getScheduledDate().isBefore(today) && session.getStatus() == RevisionStatus.PENDING) {
                session.setStatus(RevisionStatus.OVERDUE);
                revisionSessionRepository.save(session);
            }
        }

        return dueSessions.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Retrieves future upcoming revision tasks.
     */
    public List<RevisionSessionDto> getUpcomingRevisions() {
        LocalDate today = LocalDate.now();
        List<RevisionSession> upcoming = revisionSessionRepository
                .findByScheduledDateGreaterThanAndStatusOrderByScheduledDateAsc(
                        today, RevisionStatus.PENDING
                );

        return upcoming.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * Reviews a revision session using the SM-2 algorithm.
     * Updates intervals, easiness factor, next scheduled date, and logs history.
     */
    public RevisionSessionDto reviewRevision(Long sessionId, RevisionReviewRequest request) {
        if (request == null || request.getRating() == null) {
            throw new BadRequestException("Recall rating is required");
        }

        RevisionSession session = revisionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Revision session not found with id: " + sessionId));

        int prevInterval = session.getIntervalDays();
        double prevEf = session.getEasinessFactor();
        int repNum = session.getRepetitionNumber();

        RecallRating rating = request.getRating();
        int newInterval;
        double newEf = prevEf;
        int newRepNum;

        // --- SM-2 Algorithm Calculation ---
        switch (rating) {
            case AGAIN:
                // Blackout / failure: reset repetition counter, review tomorrow
                newRepNum = 0;
                newInterval = 1;
                newEf = Math.max(1.3, prevEf - 0.20);
                break;

            case HARD:
                // Difficult recall: slight interval growth, decrease EF
                newRepNum = repNum + 1;
                if (newRepNum == 1) {
                    newInterval = 1;
                } else if (newRepNum == 2) {
                    newInterval = 2;
                } else if (newRepNum == 3) {
                    newInterval = 4;
                } else {
                    newInterval = Math.max(1, (int) Math.round(prevInterval * 1.2));
                }
                newEf = Math.max(1.3, prevEf - 0.15);
                break;

            case GOOD:
                // Normal correct recall: standard intervals (1, 3, 7, 14, then interval * EF)
                newRepNum = repNum + 1;
                if (newRepNum == 1) {
                    newInterval = 1;
                } else if (newRepNum == 2) {
                    newInterval = 3;
                } else if (newRepNum == 3) {
                    newInterval = 7;
                } else if (newRepNum == 4) {
                    newInterval = 14;
                } else {
                    newInterval = Math.max(1, (int) Math.round(prevInterval * prevEf));
                }
                // EF remains constant on GOOD
                break;

            case EASY:
                // Effortless instant recall: accelerated intervals, increase EF
                newRepNum = repNum + 1;
                if (newRepNum == 1) {
                    newInterval = 2;
                } else if (newRepNum == 2) {
                    newInterval = 4;
                } else if (newRepNum == 3) {
                    newInterval = 9;
                } else if (newRepNum == 4) {
                    newInterval = 18;
                } else {
                    newInterval = Math.max(1, (int) Math.round(prevInterval * prevEf * 1.3));
                }
                newEf = Math.min(3.5, prevEf + 0.15);
                break;

            default:
                newRepNum = repNum + 1;
                newInterval = prevInterval * 2;
        }

        newEf = Math.round(newEf * 100.0) / 100.0;
        LocalDate nextDate = LocalDate.now().plusDays(newInterval);

        // Update session
        session.setRepetitionNumber(newRepNum);
        session.setIntervalDays(newInterval);
        session.setEasinessFactor(newEf);
        session.setScheduledDate(nextDate);
        session.setStatus(RevisionStatus.PENDING);
        session.setLastReviewedAt(LocalDateTime.now());

        RevisionSession updatedSession = revisionSessionRepository.save(session);

        // Log RevisionHistory
        RevisionHistory history = new RevisionHistory(
                updatedSession,
                updatedSession.getTopic(),
                rating,
                LocalDateTime.now(),
                prevInterval,
                newInterval,
                prevEf,
                newEf,
                request.getNotes()
        );
        revisionHistoryRepository.save(history);

        logger.info("Revision session {} reviewed with rating {}. Next review: {} (interval: {}d, EF: {})",
                sessionId, rating, nextDate, newInterval, newEf);

        return mapToDto(updatedSession);
    }

    /**
     * Retrieves all past revision history entries.
     */
    public List<RevisionHistoryDto> getRevisionHistory() {
        return revisionHistoryRepository.findAllByOrderByReviewedAtDesc()
                .stream().map(this::mapHistoryToDto).collect(Collectors.toList());
    }

    // --- Flashcards Subsystem ---

    public FlashcardDto createFlashcard(FlashcardCreateRequest request) {
        if (request == null) {
            throw new BadRequestException("Flashcard request cannot be null");
        }
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + request.getTopicId()));

        Flashcard card = new Flashcard(
                topic,
                request.getFrontPrompt(),
                request.getBackAnswer(),
                request.getHint()
        );

        Flashcard saved = flashcardRepository.save(card);
        return mapFlashcardToDto(saved);
    }

    public List<FlashcardDto> getFlashcardsByTopic(Long topicId) {
        return flashcardRepository.findByTopicId(topicId)
                .stream().map(this::mapFlashcardToDto).collect(Collectors.toList());
    }

    public List<FlashcardDto> getAllFlashcards() {
        return flashcardRepository.findAll()
                .stream().map(this::mapFlashcardToDto).collect(Collectors.toList());
    }

    public List<FlashcardDto> getDueFlashcards() {
        LocalDate today = LocalDate.now();
        return flashcardRepository.findByNextReviewDateLessThanEqualOrderByNextReviewDateAsc(today)
                .stream().map(this::mapFlashcardToDto).collect(Collectors.toList());
    }

    public FlashcardDto reviewFlashcard(Long cardId, FlashcardReviewRequest request) {
        Flashcard card = flashcardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard not found with id: " + cardId));

        int prevInterval = card.getIntervalDays();
        double prevEf = card.getEasinessFactor();
        int repNum = card.getRepetitionNumber();
        RecallRating rating = request.getRating();

        int newInterval;
        double newEf = prevEf;
        int newRep;

        if (rating == RecallRating.AGAIN) {
            newRep = 0;
            newInterval = 1;
            newEf = Math.max(1.3, prevEf - 0.20);
        } else if (rating == RecallRating.HARD) {
            newRep = repNum + 1;
            newInterval = newRep == 1 ? 1 : (newRep == 2 ? 2 : (int) Math.round(prevInterval * 1.2));
            newEf = Math.max(1.3, prevEf - 0.15);
        } else if (rating == RecallRating.GOOD) {
            newRep = repNum + 1;
            newInterval = newRep == 1 ? 1 : (newRep == 2 ? 3 : (newRep == 3 ? 7 : (int) Math.round(prevInterval * prevEf)));
        } else {
            newRep = repNum + 1;
            newInterval = newRep == 1 ? 2 : (newRep == 2 ? 4 : (newRep == 3 ? 9 : (int) Math.round(prevInterval * prevEf * 1.3)));
            newEf = Math.min(3.5, prevEf + 0.15);
        }

        newEf = Math.round(newEf * 100.0) / 100.0;
        LocalDate nextDate = LocalDate.now().plusDays(Math.max(1, newInterval));

        card.setRepetitionNumber(newRep);
        card.setIntervalDays(newInterval);
        card.setEasinessFactor(newEf);
        card.setNextReviewDate(nextDate);

        Flashcard savedCard = flashcardRepository.save(card);

        FlashcardReview reviewLog = new FlashcardReview(
                savedCard,
                rating,
                LocalDateTime.now(),
                request.getTimeSpentSeconds()
        );
        flashcardReviewRepository.save(reviewLog);

        return mapFlashcardToDto(savedCard);
    }

    private RevisionSessionDto mapToDto(RevisionSession s) {
        Topic t = s.getTopic();
        LocalDate today = LocalDate.now();
        boolean isOverdue = s.getScheduledDate().isBefore(today);
        long daysOverdue = isOverdue ? ChronoUnit.DAYS.between(s.getScheduledDate(), today) : 0;

        return new RevisionSessionDto(
                s.getId(),
                t.getId(),
                t.getName(),
                t.getSubject() != null ? t.getSubject().getId() : null,
                t.getSubject() != null ? t.getSubject().getName() : "General",
                t.getDifficulty(),
                s.getScheduledDate(),
                s.getRepetitionNumber(),
                s.getIntervalDays(),
                s.getEasinessFactor(),
                s.getStatus(),
                isOverdue,
                daysOverdue,
                s.getLastReviewedAt(),
                s.getCreatedAt()
        );
    }

    private RevisionHistoryDto mapHistoryToDto(RevisionHistory h) {
        Topic t = h.getTopic();
        return new RevisionHistoryDto(
                h.getId(),
                h.getRevisionSession() != null ? h.getRevisionSession().getId() : null,
                t.getId(),
                t.getName(),
                t.getSubject() != null ? t.getSubject().getName() : "General",
                h.getRating(),
                h.getReviewedAt(),
                h.getPreviousIntervalDays(),
                h.getNewIntervalDays(),
                h.getPreviousEf(),
                h.getNewEf(),
                h.getNotes()
        );
    }

    private FlashcardDto mapFlashcardToDto(Flashcard f) {
        Topic t = f.getTopic();
        boolean due = !f.getNextReviewDate().isAfter(LocalDate.now());
        return new FlashcardDto(
                f.getId(),
                t.getId(),
                t.getName(),
                t.getSubject() != null ? t.getSubject().getName() : "General",
                f.getFrontPrompt(),
                f.getBackAnswer(),
                f.getHint(),
                f.getRepetitionNumber(),
                f.getIntervalDays(),
                f.getEasinessFactor(),
                f.getNextReviewDate(),
                due
        );
    }
}

