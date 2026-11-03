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
 * if the database is empty, and guarantees default user ownership.
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
    private final FlashcardRepository flashcardRepository;
    private final RevisionSessionRepository revisionSessionRepository;
    private final UserRepository userRepository;
    private final StudySessionRepository studySessionRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public DataInitializer(SubjectRepository subjectRepository,
                           TopicRepository topicRepository,
                           QuizRepository quizRepository,
                           QuestionRepository questionRepository,
                           QuizOptionRepository quizOptionRepository,
                           com.focusflow.service.TopicPerformanceService topicPerformanceService,
                           FlashcardRepository flashcardRepository,
                           RevisionSessionRepository revisionSessionRepository,
                           UserRepository userRepository,
                           StudySessionRepository studySessionRepository,
                           org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.quizOptionRepository = quizOptionRepository;
        this.topicPerformanceService = topicPerformanceService;
        this.flashcardRepository = flashcardRepository;
        this.revisionSessionRepository = revisionSessionRepository;
        this.userRepository = userRepository;
        this.studySessionRepository = studySessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Ensure Default Student Account Exists
        User demoStudent = userRepository.findByEmail("student@focusflow.com").orElse(null);
        if (demoStudent == null) {
            demoStudent = new User(
                    "student@focusflow.com",
                    passwordEncoder.encode("Student123!"),
                    "Demo Student",
                    Role.ROLE_USER
            );
            demoStudent = userRepository.save(demoStudent);
            logger.info("Seeded starter student account: student@focusflow.com with ROLE_USER");
        } else {
            // Ensure role is normalized to ROLE_USER and user_roles are populated
            if (demoStudent.getRole() == Role.ROLE_STUDENT) {
                demoStudent.setRole(Role.ROLE_USER);
            }
            demoStudent.addRole(Role.ROLE_USER);
            demoStudent = userRepository.save(demoStudent);
        }

        // 2. Ensure Starter Subjects & Topics
        if (subjectRepository.count() == 0) {
            logger.info("Database is empty. Seeding starter subjects and topics for FocusFlow...");

            LocalDate today = LocalDate.now();

            // 1. Starter Subjects with Owner
            Subject dsa = new Subject("Data Structures & Algorithms", today.plusDays(4), Importance.HIGH, demoStudent);
            Subject os = new Subject("Operating Systems", today.plusDays(10), Importance.MEDIUM, demoStudent);
            Subject dbms = new Subject("Database Management Systems", today.plusDays(22), Importance.LOW, demoStudent);

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

            logger.info("Successfully seeded starter data: 3 subjects and 5 topics for student: {}", demoStudent.getEmail());
        } else {
            // Backfill any existing unowned subjects
            List<Subject> allSubjects = subjectRepository.findAll();
            for (Subject s : allSubjects) {
                if (s.getUser() == null) {
                    s.setUser(demoStudent);
                    subjectRepository.save(s);
                }
            }
        }

        // 3. Backfill any existing unowned study sessions
        List<StudySession> allSessions = studySessionRepository.findAll();
        for (StudySession sess : allSessions) {
            if (sess.getUser() == null) {
                sess.setUser(demoStudent);
                studySessionRepository.save(sess);
            }
        }

        // 4. Seed sample quizzes if no quizzes exist
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
                        1,
                        "In the worst case, an unbalanced BST degenerates into a linked list, resulting in O(N) time complexity."
                );
                Question savedQ1 = questionRepository.save(q1);

                quizOptionRepository.save(new QuizOption(savedQ1, "O(1)", false, 1));
                quizOptionRepository.save(new QuizOption(savedQ1, "O(log N)", false, 2));
                quizOptionRepository.save(new QuizOption(savedQ1, "O(N)", true, 3));
                quizOptionRepository.save(new QuizOption(savedQ1, "O(N log N)", false, 4));

                Question q2 = new Question(
                        savedDsaQuiz,
                        bstTopic,
                        "Which tree traversal method visits nodes in strictly non-decreasing sorted order for any valid BST?",
                        1,
                        "In-order traversal visits Left Subtree -> Root -> Right Subtree, resulting in sorted key order."
                );
                Question savedQ2 = questionRepository.save(q2);

                quizOptionRepository.save(new QuizOption(savedQ2, "Pre-order Traversal", false, 1));
                quizOptionRepository.save(new QuizOption(savedQ2, "In-order Traversal", true, 2));
                quizOptionRepository.save(new QuizOption(savedQ2, "Post-order Traversal", false, 3));
                quizOptionRepository.save(new QuizOption(savedQ2, "Level-order Traversal", false, 4));

                if (graphTopic != null) {
                    Question q3 = new Question(
                            savedDsaQuiz,
                            graphTopic,
                            "Which standard data structure is utilized in Breadth-First Search (BFS) graph traversal?",
                            1,
                            "Breadth-First Search systematically processes neighboring vertices level by level using a Queue (FIFO)."
                    );
                    Question savedQ3 = questionRepository.save(q3);

                    quizOptionRepository.save(new QuizOption(savedQ3, "Stack", false, 1));
                    quizOptionRepository.save(new QuizOption(savedQ3, "Queue", true, 2));
                    quizOptionRepository.save(new QuizOption(savedQ3, "Priority Queue", false, 3));
                    quizOptionRepository.save(new QuizOption(savedQ3, "Binary Heap", false, 4));
                }

                // Quiz 2: Operating Systems Concurrency & Deadlocks
                Subject osSubject = subjects.stream()
                        .filter(s -> s.getName().contains("Operating Systems"))
                        .findFirst()
                        .orElse(null);

                if (osSubject != null) {
                    Topic deadlockTopic = topicRepository.findBySubjectId(osSubject.getId()).stream()
                            .filter(t -> t.getName().contains("Deadlock"))
                            .findFirst()
                            .orElse(null);

                    Quiz osQuiz = new Quiz(
                            "Operating Systems Concurrency & Deadlocks",
                            "Test your understanding of synchronization primitives, deadlock avoidance, and Banker's algorithm.",
                            osSubject,
                            deadlockTopic,
                            10,
                            20,
                            12
                    );
                    Quiz savedOsQuiz = quizRepository.save(osQuiz);

                    Question oq1 = new Question(
                            savedOsQuiz,
                            deadlockTopic,
                            "Which of the following conditions is NOT one of the four necessary Coffman conditions for a deadlock?",
                            1,
                            "Paging is a memory management scheme, not a Coffman concurrency condition."
                    );
                    Question savedOq1 = questionRepository.save(oq1);

                    quizOptionRepository.save(new QuizOption(savedOq1, "Mutual Exclusion", false, 1));
                    quizOptionRepository.save(new QuizOption(savedOq1, "Hold and Wait", false, 2));
                    quizOptionRepository.save(new QuizOption(savedOq1, "No Preemption", false, 3));
                    quizOptionRepository.save(new QuizOption(savedOq1, "Paging", true, 4));

                    Question oq2 = new Question(
                            savedOsQuiz,
                            deadlockTopic,
                            "Banker's Algorithm is primarily used in operating systems for which purpose?",
                            1,
                            "Banker's Algorithm tests for safety by simulating resource allocation for predetermined maximum amounts to prevent deadlocks."
                    );
                    Question savedOq2 = questionRepository.save(oq2);

                    quizOptionRepository.save(new QuizOption(oq2, "Deadlock Detection", false, 1));
                    quizOptionRepository.save(new QuizOption(oq2, "Deadlock Avoidance", true, 2));
                    quizOptionRepository.save(new QuizOption(oq2, "Preemption of Resources", false, 3));
                    quizOptionRepository.save(new QuizOption(oq2, "Circular Wait Resolution", false, 4));
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

        // 5. Seed Starter Flashcards & Revision Sessions independently
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
                        "Left < Root < Right",
                        demoStudent
                ));
                flashcardRepository.save(new Flashcard(
                        bstTopic,
                        "What is the worst-case search complexity in an unbalanced BST vs an AVL tree?",
                        "Unbalanced BST: O(n) when degenerating into a linear chain. Balanced AVL: guaranteed O(log n).",
                        "Think of skewed degenerate trees.",
                        demoStudent
                ));

                // Seed an initial due revision session for today
                if (revisionSessionRepository.count() == 0) {
                    revisionSessionRepository.save(new RevisionSession(
                            bstTopic,
                            LocalDate.now(),
                            1,
                            1,
                            2.5,
                            RevisionStatus.PENDING,
                            demoStudent
                    ));
                }
            }

            if (schedTopic != null) {
                flashcardRepository.save(new Flashcard(
                        schedTopic,
                        "What causes the 'convoy effect' in CPU scheduling?",
                        "First-Come, First-Served (FCFS) when a CPU-bound process monopolizes the processor, causing short I/O-bound processes to wait.",
                        "Slow vehicle on a single-lane road.",
                        demoStudent
                ));
            }

            if (deadlockTopic != null) {
                flashcardRepository.save(new Flashcard(
                        deadlockTopic,
                        "What are the four Coffman conditions required for a deadlock to exist?",
                        "1. Mutual Exclusion\n2. Hold and Wait\n3. No Preemption\n4. Circular Wait",
                        "All 4 conditions must hold simultaneously.",
                        demoStudent
                ));
            }

            logger.info("Successfully seeded starter flashcards and revision session.");
        } else {
            // Backfill any existing unowned flashcards and revision sessions
            List<Flashcard> allCards = flashcardRepository.findAll();
            for (Flashcard f : allCards) {
                if (f.getUser() == null) {
                    f.setUser(demoStudent);
                    flashcardRepository.save(f);
                }
            }
            List<RevisionSession> allRevs = revisionSessionRepository.findAll();
            for (RevisionSession r : allRevs) {
                if (r.getUser() == null) {
                    r.setUser(demoStudent);
                    revisionSessionRepository.save(r);
                }
            }
        }
    }
}
