package com.focusflow.service;

import com.focusflow.entity.Subject;
import com.focusflow.entity.Topic;
import com.focusflow.exception.BadRequestException;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.SubjectRepository;
import com.focusflow.repository.TopicRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class handling business logic for Topic management with multi-user isolation.
 */
@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final RevisionService revisionService;

    public TopicService(TopicRepository topicRepository,
                        SubjectRepository subjectRepository,
                        @Lazy RevisionService revisionService) {
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.revisionService = revisionService;
    }

    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    public List<Topic> getTopicsBySubjectId(Long subjectId) {
        return getTopicsBySubjectId(subjectId, null);
    }

    public List<Topic> getTopicsBySubjectId(Long subjectId, Long userId) {
        if (subjectId == null) {
            throw new BadRequestException("Subject ID cannot be null");
        }
        if (userId != null) {
            return topicRepository.findBySubjectIdAndSubjectUserId(subjectId, userId);
        }
        return topicRepository.findBySubjectId(subjectId);
    }

    public Topic getTopicById(Long id) {
        if (id == null) {
            throw new BadRequestException("Topic ID cannot be null");
        }
        return topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));
    }

    public Topic getTopicById(Long id, Long userId) {
        if (id == null) {
            throw new BadRequestException("Topic ID cannot be null");
        }
        if (userId == null) {
            return getTopicById(id);
        }
        return topicRepository.findByIdAndSubjectUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Topic not found with id: " + id));
    }

    public Topic createTopic(Topic topic) {
        return createTopic(topic, null);
    }

    public Topic createTopic(Topic topic, Long userId) {
        if (topic == null) {
            throw new BadRequestException("Topic data cannot be null");
        }
        if (topic.getSubject() == null || topic.getSubject().getId() == null) {
            throw new BadRequestException("A valid Subject must be selected for the topic");
        }

        Long subjectId = topic.getSubject().getId();
        Subject subject;
        if (userId != null) {
            subject = subjectRepository.findByIdAndUserId(subjectId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found or access denied for ID: " + subjectId));
        } else {
            subject = subjectRepository.findById(subjectId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));
        }

        topic.setSubject(subject);
        return topicRepository.save(topic);
    }

    public Topic updateTopic(Long id, Topic updatedTopic) {
        return updateTopic(id, updatedTopic, null);
    }

    public Topic updateTopic(Long id, Topic updatedTopic, Long userId) {
        if (id == null) {
            throw new BadRequestException("Topic ID cannot be null");
        }
        if (updatedTopic == null) {
            throw new BadRequestException("Updated topic data cannot be null");
        }
        Topic existingTopic = getTopicById(id, userId);
        existingTopic.setName(updatedTopic.getName());
        existingTopic.setDifficulty(updatedTopic.getDifficulty());
        existingTopic.setEstimatedMinutes(updatedTopic.getEstimatedMinutes());
        existingTopic.setCompleted(updatedTopic.isCompleted());

        if (updatedTopic.getSubject() != null && updatedTopic.getSubject().getId() != null) {
            Long subjectId = updatedTopic.getSubject().getId();
            Subject subject = (userId != null)
                    ? subjectRepository.findByIdAndUserId(subjectId, userId).orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId))
                    : subjectRepository.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectId));
            existingTopic.setSubject(subject);
        }

        Topic saved = topicRepository.save(existingTopic);
        if (saved.isCompleted() && revisionService != null) {
            try {
                revisionService.createInitialRevision(saved);
            } catch (Exception ex) {
                // Silently safeguard topic save from revision failure
            }
        }
        return saved;
    }

    public Topic toggleTopicCompletion(Long id) {
        return toggleTopicCompletion(id, null);
    }

    public Topic toggleTopicCompletion(Long id, Long userId) {
        if (id == null) {
            throw new BadRequestException("Topic ID cannot be null");
        }
        Topic existingTopic = getTopicById(id, userId);
        boolean wasCompleted = existingTopic.isCompleted();
        existingTopic.setCompleted(!wasCompleted);
        Topic saved = topicRepository.save(existingTopic);

        if (!wasCompleted && saved.isCompleted() && revisionService != null) {
            try {
                revisionService.createInitialRevision(saved);
            } catch (Exception ex) {
                // Silently safeguard
            }
        }
        return saved;
    }

    public void deleteTopic(Long id) {
        deleteTopic(id, null);
    }

    public void deleteTopic(Long id, Long userId) {
        if (id == null) {
            throw new BadRequestException("Topic ID cannot be null");
        }
        Topic existingTopic = getTopicById(id, userId);
        topicRepository.delete(existingTopic);
    }
}
