package com.focusflow.repository;

import com.focusflow.entity.PasswordResetToken;
import com.focusflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByUserAndUsedFalse(User user);

    @Modifying
    void deleteByExpiryDateBefore(LocalDateTime dateTime);

    @Modifying
    void deleteByUser(User user);
}

