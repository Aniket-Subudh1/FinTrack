package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByEmail(String email);

    void deleteByEmail(String email);

    // Find and delete expired tokens
    @Transactional
    void deleteByExpirationTimeBefore(LocalDateTime now);
}
