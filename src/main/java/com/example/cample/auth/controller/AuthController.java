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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

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

    // ✅ 로그인ID 형식 재사용(회원가입과 동일 규칙)
    private static final Pattern LOGIN_ID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_\\-]{4,20}$");

    // ✅ ID 중복확인 API (비인증 허용 필요)
    @GetMapping("/id/check")
    public ResponseEntity<Map<String, Object>> checkLoginId(@RequestParam String loginId) {
        boolean valid = loginId != null && LOGIN_ID_PATTERN.matcher(loginId).matches();
        if (!valid) {
            return ResponseEntity.badRequest().body(
                    Map.of("loginId", loginId, "available", false, "reason", "INVALID_FORMAT")
            );
        }
        boolean available = userService.isLoginIdAvailable(loginId);
        return ResponseEntity.ok(Map.of("loginId", loginId, "available", available));
    }

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
        var tb = authService.signupLocal(
                req.loginId(),
                req.email(),
                req.password(),
                req.passwordConfirm(),
                req.code(),
                res
        );
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

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/unregister")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                HttpServletResponse res) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        userService.deleteUserById(principal.getId());
        CookieUtils.addHttpOnlyCookie(res, refreshCookieName, "", 0, refreshCookieSecure, refreshCookieDomain);
        return ResponseEntity.noContent().build();
    }
}
