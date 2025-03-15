package com.personalfinance.auth.repository;

import com.personalfinance.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByEmail(String email);

    Optional<PasswordResetToken> findByEmailAndOtp(String email, String otp);

    @Transactional
    void deleteByEmail(String email);

    @Transactional
    void deleteByExpirationTimeBefore(LocalDateTime dateTime);
}