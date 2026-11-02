package com.focusflow.service;

import com.focusflow.entity.Subject;
import com.focusflow.exception.BadRequestException;
import com.focusflow.exception.ResourceNotFoundException;
import com.focusflow.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class handling business logic for Subject management.
 */
@Service
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final com.focusflow.repository.ExamReadinessRepository examReadinessRepository;

    public SubjectService(SubjectRepository subjectRepository,
                          com.focusflow.repository.ExamReadinessRepository examReadinessRepository) {
        this.subjectRepository = subjectRepository;
        this.examReadinessRepository = examReadinessRepository;
    }

    public List<Subject> getAllSubjects() {
        return subjectRepository.findAll();
    }

    public Subject getSubjectById(Long id) {
        if (id == null) {
            throw new BadRequestException("Subject ID cannot be null");
        }
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + id));
    }

    public Subject createSubject(Subject subject) {
        if (subject == null) {
            throw new BadRequestException("Subject data cannot be null");
        }
        return subjectRepository.save(subject);
    }

    public Subject updateSubject(Long id, Subject updatedSubject) {
        if (id == null) {
            throw new BadRequestException("Subject ID cannot be null");
        }
        if (updatedSubject == null) {
            throw new BadRequestException("Updated subject data cannot be null");
        }
        Subject existingSubject = getSubjectById(id);
        existingSubject.setName(updatedSubject.getName());
        existingSubject.setExamDate(updatedSubject.getExamDate());
        existingSubject.setImportance(updatedSubject.getImportance());
        return subjectRepository.save(existingSubject);
    }

    public void deleteSubject(Long id) {
        if (id == null) {
            throw new BadRequestException("Subject ID cannot be null");
        }
        Subject existingSubject = getSubjectById(id);
        examReadinessRepository.deleteBySubjectId(id);
        subjectRepository.delete(existingSubject);
    }
}
