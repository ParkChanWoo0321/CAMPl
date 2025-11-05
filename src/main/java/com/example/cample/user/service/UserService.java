// src/main/java/com/example/cample/user/service/UserService.java
package com.example.cample.user.service;

import com.example.cample.common.exception.ApiException;
import com.example.cample.user.domain.AuthProvider;
import com.example.cample.user.domain.User;
import com.example.cample.user.domain.UserStatus;
import com.example.cample.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ 로그인ID 사용 가능 여부
    @Transactional(readOnly = true)
    public boolean isLoginIdAvailable(String loginId) {
        if (loginId == null || loginId.isBlank()) return false;
        return !userRepository.existsByLoginId(loginId);
    }

    public User createLocalUser(String loginId, String name, String email, String rawPassword) {
        if (userRepository.existsByLoginId(loginId))
            throw new ApiException(HttpStatus.CONFLICT, "이미 사용 중인 로그인ID");
        if (userRepository.existsByEmail(email))
            throw new ApiException(HttpStatus.CONFLICT, "이미 가입된 이메일");

        User u = User.builder()
                .loginId(loginId)
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .provider(AuthProvider.LOCAL)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(u);
    }

    // 카카오: 이메일 기준 upsert + 이름 갱신
    public User upsertKakaoUser(String kakaoId, String email, String displayName) {
        return userRepository.findByEmail(email).map(u -> {
            if (displayName != null && !displayName.isBlank() && !displayName.equals(u.getName())) {
                u.setName(displayName.trim());
                return userRepository.save(u);
            }
            return u;
        }).orElseGet(() -> {
            String loginId = "kakao_" + kakaoId;
            User u = User.builder()
                    .loginId(loginId)
                    .name(displayName != null && !displayName.isBlank() ? displayName.trim() : "카카오사용자")
                    .email(email)
                    .password(null)
                    .provider(AuthProvider.KAKAO)
                    .status(UserStatus.ACTIVE)
                    .build();
            return userRepository.save(u);
        });
    }

    public void updateLastLogin(User u) {
        u.setLastLoginAt(LocalDateTime.now());
        userRepository.save(u);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "사용자 없음"));
    }

    @Transactional
    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) return;
        userRepository.deleteById(id);
    }
}
