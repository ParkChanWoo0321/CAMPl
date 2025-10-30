package com.example.cample.auth.repo;

import com.example.cample.auth.domain.EmailVerification;
import com.example.cample.auth.domain.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByIdDesc(String email, VerificationPurpose purpose);
}
