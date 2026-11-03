package com.focusflow.repository;

import com.focusflow.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Subject entity.
 * Provides standard CRUD and multi-user tenant-scoped operations.
 */
@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByUser_Id(Long userId);

    default List<Subject> findByUserId(Long userId) {
        return findByUser_Id(userId);
    }

    @Query("SELECT s FROM Subject s WHERE s.id = :id AND s.user.id = :userId")
    Optional<Subject> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subject s WHERE s.id = :id AND s.user.id = :userId")
    boolean existsByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
