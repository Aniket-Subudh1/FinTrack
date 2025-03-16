package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findFirstByEmail(String email);

    void deleteByEmail(String email);

    // Find and delete expired OTPs
    @Transactional
    void deleteByExpirationTimeBefore(LocalDateTime now);
}
