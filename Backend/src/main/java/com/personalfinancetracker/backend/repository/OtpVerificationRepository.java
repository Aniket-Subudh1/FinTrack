package com.personalfinancetracker.backend.repository;

import com.personalfinancetracker.backend.entities.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findFirstByEmail(String email);  // Ensure this returns a unique record
    void deleteByEmail(String email);
}
