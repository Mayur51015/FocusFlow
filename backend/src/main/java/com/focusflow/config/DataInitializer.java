package com.focusflow.config;

import com.focusflow.entity.*;
import com.focusflow.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * DataInitializer seeds sample subjects, topics, and starter quizzes upon initial startup
 * if the database is empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final com.focusflow.service.TopicPerformanceService topicPerformanceService;
    private final com.focusflow.repository.FlashcardRepository flashcardRepository;
    private final com.focusflow.repository.RevisionSessionRepository revisionSessionRepository;

    public DataInitializer(SubjectRepository subjectRepository,
                           TopicRepository topicRepository,
                           QuizRepository quizRepository,
                           QuestionRepository questionRepository,
                           QuizOptionRepository quizOptionRepository,
                           com.focusflow.service.TopicPerformanceService topicPerformanceService,
                           com.focusflow.repository.FlashcardRepository flashcardRepository,
                           com.focusflow.repository.RevisionSessionRepository revisionSessionRepository) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizOptionRepository = quizOptionRepository;
        this.topicPerformanceService = topicPerformanceService;
        this.flashcardRepository = flashcardRepository;
        this.revisionSessionRepository = revisionSessionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (subjectRepository.count() == 0) {
            logger.info("Database is empty. Seeding starter subjects and topics for FocusFlow...");

            LocalDate today = LocalDate.now();

            // 1. Starter Subjects
            Subject dsa = new Subject("Data Structures & Algorithms", today.plusDays(4), Importance.HIGH);
            Subject os = new Subject("Operating Systems", today.plusDays(10), Importance.MEDIUM);
            Subject dbms = new Subject("Database Management Systems", today.plusDays(22), Importance.LOW);

            List<Subject> savedSubjects = subjectRepository.saveAll(List.of(dsa, os, dbms));
            Subject savedDsa = savedSubjects.get(0);
            Subject savedOs = savedSubjects.get(1);
            Subject savedDbms = savedSubjects.get(2);

            // 2. Starter Topics
            Topic t1 = new Topic("Binary Search Trees", savedDsa, Difficulty.HARD, 90, false);
            Topic t2 = new Topic("Graph Traversals (BFS & DFS)", savedDsa, Difficulty.HARD, 120, false);
            Topic t3 = new Topic("CPU Scheduling Algorithms", savedOs, Difficulty.MEDIUM, 60, false);
            Topic t4 = new Topic("Deadlock Prevention & Avoidance", savedOs, Difficulty.MEDIUM, 45, false);
            Topic t5 = new Topic("SQL Joins & Indexing", savedDbms, Difficulty.EASY, 45, false);

            topicRepository.saveAll(List.of(t1, t2, t3, t4, t5));

            logger.info("Successfully seeded starter data: 3 subjects and 5 topics.");
        }

        // Seed sample quizzes if no quizzes exist
        if (quizRepository.count() == 0) {
            List<Subject> subjects = subjectRepository.findAll();
            if (!subjects.isEmpty()) {
                Subject dsaSubject = subjects.stream()
                        .filter(s -> s.getName().contains("Data Structures"))
                        .findFirst()
                        .orElse(subjects.get(0));

                List<Topic> dsaTopics = topicRepository.findBySubjectId(dsaSubject.getId());
                Topic bstTopic = dsaTopics.stream()
                        .filter(t -> t.getName().contains("Binary Search Trees"))
                        .findFirst()
                        .orElse(null);

                Topic graphTopic = dsaTopics.stream()
                        .filter(t -> t.getName().contains("Graph Traversals"))
                        .findFirst()
                        .orElse(null);

                // Quiz 1: DSA Trees & Graphs Mastery
                Quiz dsaQuiz = new Quiz(
                        "DSA Trees & Graphs Mastery Quiz",
                        "Evaluate your algorithmic problem solving in Binary Search Trees and Graph Traversals.",
                        dsaSubject,
                        bstTopic,
                        15,
                        30,
                        20
                );
                Quiz savedDsaQuiz = quizRepository.save(dsaQuiz);

                Question q1 = new Question(
                        savedDsaQuiz,
                        bstTopic,
                        "What is the worst-case time complexity of searching for an element in an unbalanced Binary Search Tree?",
                        10,
                        "In an unbalanced BST that degenerates into a skewed linear chain, search operations degrade to O(n)."
                );
                questionRepository.save(q1);
                quizOptionRepository.save(new QuizOption(q1, "O(1)", false, 1));
                quizOptionRepository.save(new QuizOption(q1, "O(log n)", false, 2));
                quizOptionRepository.save(new QuizOption(q1, "O(n)", true, 3));
                quizOptionRepository.save(new QuizOption(q1, "O(n log n)", false, 4));

                Question q2 = new Question(
                        savedDsaQuiz,
                        graphTopic,
                        "Which fundamental data structure is utilized to implement Breadth-First Search (BFS) on an unweighted graph?",
                        10,
                        "BFS explores vertices level-by-level using a First-In, First-Out (FIFO) Queue."
                );
                questionRepository.save(q2);
                quizOptionRepository.save(new QuizOption(q2, "LIFO Stack", false, 1));
                quizOptionRepository.save(new QuizOption(q2, "FIFO Queue", true, 2));
                quizOptionRepository.save(new QuizOption(q2, "Min-Heap", false, 3));
                quizOptionRepository.save(new QuizOption(q2, "Hash Map", false, 4));

                Question q3 = new Question(
                        savedDsaQuiz,
                        bstTopic,
                        "Which traversal method visits the nodes of a valid Binary Search Tree in strictly ascending order?",
                        10,
                        "In-order traversal (Left subtree, Root, Right subtree) yields BST keys in sorted ascending sequence."
                );
                questionRepository.save(q3);
                quizOptionRepository.save(new QuizOption(q3, "Pre-order Traversal", false, 1));
                quizOptionRepository.save(new QuizOption(q3, "Post-order Traversal", false, 2));
                quizOptionRepository.save(new QuizOption(q3, "In-order Traversal", true, 3));
                quizOptionRepository.save(new QuizOption(q3, "Level-order Traversal", false, 4));

                // Quiz 2: OS CPU Scheduling & Deadlocks
                Subject osSubject = subjects.stream()
                        .filter(s -> s.getName().contains("Operating Systems"))
                        .findFirst()
                        .orElse(null);

                if (osSubject != null) {
                    List<Topic> osTopics = topicRepository.findBySubjectId(osSubject.getId());
                    Topic schedTopic = osTopics.stream()
                            .filter(t -> t.getName().contains("CPU Scheduling"))
                            .findFirst()
                            .orElse(null);

                    Topic deadlockTopic = osTopics.stream()
                            .filter(t -> t.getName().contains("Deadlock"))
                            .findFirst()
                            .orElse(null);

                    Quiz osQuiz = new Quiz(
                            "Operating Systems Scheduling & Concurrency Quiz",
                            "Assess your understanding of CPU scheduling algorithms, context switches, and deadlock conditions.",
                            osSubject,
                            schedTopic,
                            10,
                            20,
                            10
                    );
                    Quiz savedOsQuiz = quizRepository.save(osQuiz);

                    Question oq1 = new Question(
                            savedOsQuiz,
                            schedTopic,
                            "Which CPU scheduling algorithm is most prone to the 'convoy effect'?",
                            10,
                            "First-Come, First-Served (FCFS) causes short CPU bursts to stall behind long running processes."
                    );
                    questionRepository.save(oq1);
                    quizOptionRepository.save(new QuizOption(oq1, "Round Robin", false, 1));
                    quizOptionRepository.save(new QuizOption(oq1, "First-Come, First-Served (FCFS)", true, 2));
                    quizOptionRepository.save(new QuizOption(oq1, "Shortest Job First (SJF)", false, 3));
                    quizOptionRepository.save(new QuizOption(oq1, "Multi-Level Feedback Queue", false, 4));

                    Question oq2 = new Question(
                            savedOsQuiz,
                            deadlockTopic,
                            "Which of the following is NOT one of the four necessary Coffman conditions required for a deadlock?",
                            10,
                            "'No preemption' is the necessary condition. If preemption is allowed, deadlocks cannot persist."
                    );
                    questionRepository.save(oq2);
                    quizOptionRepository.save(new QuizOption(oq2, "Mutual Exclusion", false, 1));
                    quizOptionRepository.save(new QuizOption(oq2, "Hold and Wait", false, 2));
                    quizOptionRepository.save(new QuizOption(oq2, "Preemption of Resources", true, 3));
                    quizOptionRepository.save(new QuizOption(oq2, "Circular Wait", false, 4));
                }

                logger.info("Successfully seeded starter assessment quizzes and questions.");
            }

            try {
                topicPerformanceService.evaluateAllTopics();
                logger.info("Initialized topic performance evaluations across active topics.");
            } catch (Exception ex) {
                logger.warn("Could not auto-evaluate topic performance at startup: {}", ex.getMessage());
            }
        }

        // 4. Seed Starter Flashcards & Revision Sessions independently
        if (flashcardRepository.count() == 0) {
            Topic bstTopic = topicRepository.findAll().stream()
                    .filter(t -> t.getName().contains("Binary Search Trees"))
                    .findFirst().orElse(null);
            Topic schedTopic = topicRepository.findAll().stream()
                    .filter(t -> t.getName().contains("CPU Scheduling"))
                    .findFirst().orElse(null);
            Topic deadlockTopic = topicRepository.findAll().stream()
                    .filter(t -> t.getName().contains("Deadlock"))
                    .findFirst().orElse(null);

            if (bstTopic != null) {
                flashcardRepository.save(new Flashcard(
                        bstTopic,
                        "What fundamental invariant must hold for every node in a valid Binary Search Tree?",
                        "All keys in the left subtree must be strictly less than the node's key, and all keys in the right subtree must be strictly greater than the node's key.",
                        "Left < Root < Right"
                ));
                flashcardRepository.save(new Flashcard(
                        bstTopic,
                        "What is the worst-case search complexity in an unbalanced BST vs an AVL tree?",
                        "Unbalanced BST: O(n) when degenerating into a linear chain. Balanced AVL: guaranteed O(log n).",
                        "Think of skewed degenerate trees."
                ));

                // Seed an initial due revision session for today
                if (revisionSessionRepository.count() == 0) {
                    revisionSessionRepository.save(new RevisionSession(
                            bstTopic,
                            LocalDate.now(),
                            1,
                            1,
                            2.5,
                            RevisionStatus.PENDING
                    ));
                }
            }

            if (schedTopic != null) {
                flashcardRepository.save(new Flashcard(
                        schedTopic,
                        "What causes the 'convoy effect' in CPU scheduling?",
                        "First-Come, First-Served (FCFS) when a CPU-bound process monopolizes the processor, causing short I/O-bound processes to wait.",
                        "Slow vehicle on a single-lane road."
                ));
            }

            if (deadlockTopic != null) {
                flashcardRepository.save(new Flashcard(
                        deadlockTopic,
                        "What are the four Coffman conditions required for a deadlock to exist?",
                        "1. Mutual Exclusion\n2. Hold and Wait\n3. No Preemption\n4. Circular Wait",
                        "All 4 conditions must hold simultaneously."
                ));
            }

            logger.info("Successfully seeded starter flashcards and revision session.");
        }
    }
}
