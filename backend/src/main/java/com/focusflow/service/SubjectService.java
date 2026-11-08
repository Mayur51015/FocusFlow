package com.focusflow.service;

import com.focusflow.entity.Subject;
import com.focusflow.entity.User;
import com.focusflow.exception.BadRequestException;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.ExamReadinessRepository;
import com.focusflow.repository.SubjectRepository;
import com.focusflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class handling business logic for Subject management with multi-user isolation.
 */
@Service
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final ExamReadinessRepository examReadinessRepository;
    private final UserRepository userRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          ExamReadinessRepository examReadinessRepository,
                          UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.examReadinessRepository = examReadinessRepository;
        this.userRepository = userRepository;
    }

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public List<Subject> getUserSubjects(Long userId) {
        if (userId == null) {
            return getAllSubjects();
        }
        return subjectRepository.findByUserId(userId);
    }

    public Subject getSubjectById(Long id) {
        if (id == null) {
            throw new BadRequestException("Subject ID cannot be null");
        }
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
    }

    public Subject getSubjectById(Long id, Long userId) {
        if (id == null) {
            throw new BadRequestException("Subject ID cannot be null");
        }
        if (userId == null) {
            return getSubjectById(id);
        }
        return subjectRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
    }

    public Subject createSubject(Subject subject) {
        return createSubject(subject, null);
    }

    public Subject createSubject(Subject subject, Long userId) {
        if (subject == null) {
            throw new BadRequestException("Subject data cannot be null");
        }
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
            subject.setUser(user);
        } else if (subject.getUser() == null) {
            userRepository.findAll().stream().findFirst().ifPresent(subject::setUser);
        }
        return subjectRepository.save(subject);
    }

    public Subject updateSubject(Long id, Subject updatedSubject) {
        return updateSubject(id, updatedSubject, null);
    }

    public Subject updateSubject(Long id, Subject updatedSubject, Long userId) {
        if (id == null) {
            throw new BadRequestException("Subject ID cannot be null");
        }
        if (updatedSubject == null) {
            throw new BadRequestException("Updated subject data cannot be null");
        }
        Subject existingSubject = getSubjectById(id, userId);
        existingSubject.setName(updatedSubject.getName());
        existingSubject.setExamDate(updatedSubject.getExamDate());
        existingSubject.setImportance(updatedSubject.getImportance());
        return subjectRepository.save(existingSubject);
    }

    public void deleteSubject(Long id) {
        deleteSubject(id, null);
    }

    public void deleteSubject(Long id, Long userId) {
        if (id == null) {
            throw new BadRequestException("Subject ID cannot be null");
        }
        Subject existingSubject = getSubjectById(id, userId);
        examReadinessRepository.deleteBySubjectId(id);
        subjectRepository.delete(existingSubject);
    }
}
