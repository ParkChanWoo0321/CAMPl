package com.example.cample.auth.service;

import com.example.cample.auth.domain.EmailVerification;
import com.example.cample.auth.domain.VerificationPurpose;
import com.example.cample.auth.repo.EmailVerificationRepository;
import com.example.cample.common.exception.ApiException;
import com.example.cample.mail.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service @RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationRepository repo;
    private final MailService mail;

    @Value("${app.school-email-domain:}")
    private String schoolEmailDomain;

    public void sendCode(String email, VerificationPurpose purpose) {
        if (schoolEmailDomain != null && !schoolEmailDomain.isBlank()) {
            String dom = "@" + schoolEmailDomain.toLowerCase();
            if (!email.toLowerCase().endsWith(dom)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "학교 이메일만 사용 가능합니다.");
            }
        }
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        EmailVerification ev = EmailVerification.builder()
                .email(email)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        repo.save(ev);
        mail.send(email, "[Cample] 이메일 인증코드", "인증코드: " + code + " (10분 유효)");
    }

    public void verify(String email, VerificationPurpose purpose, String code) {
        EmailVerification ev = repo.findTopByEmailAndPurposeOrderByIdDesc(email, purpose)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "코드를 먼저 발급하세요."));
        if (ev.getVerifiedAt() != null) throw new ApiException(HttpStatus.BAD_REQUEST, "이미 사용된 코드입니다.");
        if (LocalDateTime.now().isAfter(ev.getExpiresAt())) throw new ApiException(HttpStatus.BAD_REQUEST, "코드가 만료되었습니다.");
        if (!ev.getCode().equals(code)) throw new ApiException(HttpStatus.BAD_REQUEST, "코드가 일치하지 않습니다.");
        ev.setVerifiedAt(LocalDateTime.now());
        repo.save(ev);
    }
}
