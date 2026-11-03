package com.focusflow.repository;

import com.focusflow.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    int countByEmailAndSuccessFalseAndAttemptedAtAfter(String email, LocalDateTime after);

    int countByIpAddressAndSuccessFalseAndAttemptedAtAfter(String ipAddress, LocalDateTime after);

    List<LoginAttempt> findTop50ByOrderByAttemptedAtDesc();

    List<LoginAttempt> findTop20BySuccessFalseOrderByAttemptedAtDesc();
}

