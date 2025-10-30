package com.example.cample.auth.service;

import com.example.cample.auth.domain.VerificationPurpose;
import com.example.cample.common.exception.ApiException;
import com.example.cample.user.domain.User;
import com.example.cample.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class RecoveryService {
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String findLoginId(String email, String code) {
        emailVerificationService.verify(email, VerificationPurpose.FIND_ID, code);
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return u.getLoginId();
    }

    public void resetPassword(String email, String code, String newPassword) {
        emailVerificationService.verify(email, VerificationPurpose.RESET_PASSWORD, code);
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        u.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(u);
    }
}
