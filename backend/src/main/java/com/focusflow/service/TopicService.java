package com.focusflow.service;

import com.focusflow.entity.Subject;
import com.focusflow.entity.Topic;
import com.focusflow.exception.BadRequestException;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.SubjectRepository;
import com.focusflow.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class handling business logic for Topic management.
 */
@Service
@Transactional
public class TopicService {

    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final RevisionService revisionService;

    public TopicService(TopicRepository topicRepository,
                        SubjectRepository subjectRepository,
                        @org.springframework.context.annotation.Lazy RevisionService revisionService) {
        this.topicRepository = topicRepository;
        this.subjectRepository = subjectRepository;
        this.revisionService = revisionService;
    }

    public List<Topic> getAllTopics() {
        return topicRepository.findAll();
    }

    public List<Topic> getTopicsBySubjectId(Long subjectId) {
        if (subjectId == null) {
            throw new BadRequestException("Subject ID cannot be null");
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

    public Topic createTopic(Topic topic) {
        if (topic == null) {
            throw new BadRequestException("Topic data cannot be null");
        }
        if (topic.getSubject() == null || topic.getSubject().getId() == null) {
            throw new BadRequestException("A valid Subject must be selected for the topic");
        }
        Subject subject = subjectRepository.findById(topic.getSubject().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + topic.getSubject().getId()));
        
        topic.setSubject(subject);
        return topicRepository.save(topic);
    }

    public Topic updateTopic(Long id, Topic updatedTopic) {
        if (id == null) {
            throw new BadRequestException("Topic ID cannot be null");
        }
        if (updatedTopic == null) {
            throw new BadRequestException("Updated topic data cannot be null");
        }
        Topic existingTopic = getTopicById(id);
        existingTopic.setName(updatedTopic.getName());
        existingTopic.setDifficulty(updatedTopic.getDifficulty());
        existingTopic.setEstimatedMinutes(updatedTopic.getEstimatedMinutes());
        existingTopic.setCompleted(updatedTopic.isCompleted());

        if (updatedTopic.getSubject() != null && updatedTopic.getSubject().getId() != null) {
            Subject subject = subjectRepository.findById(updatedTopic.getSubject().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + updatedTopic.getSubject().getId()));
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
        if (id == null) {
            throw new BadRequestException("Topic ID cannot be null");
        }
        Topic existingTopic = getTopicById(id);
        existingTopic.setCompleted(!existingTopic.isCompleted());
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

    public void deleteTopic(Long id) {
        if (id == null) {
            throw new BadRequestException("Topic ID cannot be null");
        }
        Topic existingTopic = getTopicById(id);
        topicRepository.delete(existingTopic);
    }
}
