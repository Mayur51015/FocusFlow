package com.focusflow.config;

import com.focusflow.entity.Difficulty;
import com.focusflow.entity.Importance;
import com.focusflow.entity.Subject;
import com.focusflow.entity.Topic;
import com.focusflow.repository.SubjectRepository;
import com.focusflow.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * DataInitializer seeds sample subjects and topics upon initial startup
 * if the database is empty. This allows seamless demonstration during interviews and viva.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    public DataInitializer(SubjectRepository subjectRepository, TopicRepository topicRepository) {
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
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
    }
}
