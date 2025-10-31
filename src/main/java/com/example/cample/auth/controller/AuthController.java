// src/main/java/com/example/cample/auth/controller/AuthController.java
package com.example.cample.auth.controller;

import com.example.cample.auth.domain.VerificationPurpose;
import com.example.cample.auth.dto.*;
import com.example.cample.auth.service.AuthService;
import com.example.cample.auth.service.EmailVerificationService;
import com.example.cample.auth.service.RecoveryService;
import com.example.cample.common.util.CookieUtils;
import com.example.cample.security.model.CustomUserPrincipal;
import com.example.cample.user.domain.User;
import com.example.cample.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;         // ★ 추가
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;
    private final RecoveryService recoveryService;
    private final UserService userService;

    @Value("${app.security.refresh-cookie-name:REFRESH_TOKEN}")
    private String refreshCookieName;
    @Value("${app.security.refresh-cookie-domain:}")
    private String refreshCookieDomain;
    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @PostMapping("/email/send-code")
    public ResponseEntity<Void> sendCode(@Valid @RequestBody SendEmailCodeRequest req) {
        emailVerificationService.sendCode(req.email(), req.purpose());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/verify-code")
    public ResponseEntity<Void> verifyCode(@Valid @RequestBody VerifyEmailCodeRequest req) {
        emailVerificationService.verify(req.email(), req.purpose(), req.code());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signup")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest req, HttpServletResponse res) {
        var tb = authService.signupLocal(req.name(), req.loginId(), req.schoolEmail(),
                req.password(), req.passwordConfirm(), req.code(), res);
        User u = tb.user();
        return ResponseEntity.ok(new TokenResponse(
                tb.accessToken(), u.getId(), u.getLoginId(), u.getName(), u.getEmail(), u.getProvider().name()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req, HttpServletResponse res) {
        var tb = authService.loginLocal(req.loginId(), req.password(), res);
        User u = tb.user();
        return ResponseEntity.ok(new TokenResponse(
                tb.accessToken(), u.getId(), u.getLoginId(), u.getName(), u.getEmail(), u.getProvider().name()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse res) {
        String rt = CookieUtils.getCookie(request, refreshCookieName);
        var tb = authService.refresh(rt, res);
        User u = tb.user();
        return ResponseEntity.ok(new TokenResponse(
                tb.accessToken(), u.getId(), u.getLoginId(), u.getName(), u.getEmail(), u.getProvider().name()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse res) {
        authService.logout(res);
        return ResponseEntity.noContent().build();
    }

    // 목적별 단축 엔드포인트
    @PostMapping("/email/send-code/signup")
    public ResponseEntity<Void> sendSignup(@RequestBody String email) {
        emailVerificationService.sendCode(email, VerificationPurpose.SIGNUP);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/send-code/find-id")
    public ResponseEntity<Void> sendFindId(@RequestBody String email) {
        emailVerificationService.sendCode(email, VerificationPurpose.FIND_ID);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email/send-code/reset-password")
    public ResponseEntity<Void> sendReset(@RequestBody String email) {
        emailVerificationService.sendCode(email, VerificationPurpose.RESET_PASSWORD);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recovery/find-id")
    public ResponseEntity<String> findId(@Valid @RequestBody FindIdRequest req) {
        return ResponseEntity.ok(recoveryService.findLoginId(req.email(), req.code()));
    }

    @PostMapping("/recovery/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        recoveryService.resetPassword(req.email(), req.code(), req.newPassword());
        return ResponseEntity.ok().build();
    }

    // ★ 본인 계정 하드 삭제 + 리프레시 쿠키 제거
    @PreAuthorize("isAuthenticated()") // 이 메서드는 반드시 인증 필요
    @DeleteMapping("/unregister")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                HttpServletResponse res) {
        // 널가드: 혹시 설정 누락 시 401로 반환
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        userService.deleteUserById(principal.getId()); // 하드 삭제
        CookieUtils.addHttpOnlyCookie(res, refreshCookieName, "", 0, refreshCookieSecure, refreshCookieDomain); // 리프레시 쿠키 제거
        return ResponseEntity.noContent().build();
    }
}
