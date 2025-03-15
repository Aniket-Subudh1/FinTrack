package com.personalfinance.auth.repository;

import com.personalfinance.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByEmail(String email);

    @Transactional
    void deleteByEmail(String email);

    @Transactional
    void deleteByExpirationTimeBefore(LocalDateTime dateTime);
}