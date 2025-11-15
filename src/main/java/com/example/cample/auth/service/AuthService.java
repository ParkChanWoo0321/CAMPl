// src/main/java/com/example/cample/auth/service/AuthService.java
package com.example.cample.auth.service;

import com.example.cample.auth.domain.VerificationPurpose;
import com.example.cample.auth.service.EmailVerificationService;
import com.example.cample.common.exception.ApiException;
import com.example.cample.common.util.CookieUtils;
import com.example.cample.security.JwtTokenProvider;
import com.example.cample.user.domain.User;
import com.example.cample.user.repo.UserRepository;
import com.example.cample.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailVerificationService emailVerificationService;

    @Value("${app.security.refresh-cookie-name:REFRESH_TOKEN}")
    private String refreshCookieName;

    @Value("${app.security.refresh-cookie-domain:localhost}")
    private String refreshCookieDomain;

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    // name 입력을 받지 않으므로, name은 loginId로 대체 저장
    public TokenBundle signupLocal(String loginId, String email, String password,
                                   String passwordConfirm, String code,
                                   HttpServletResponse res) {
        if (!password.equals(passwordConfirm)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "비밀번호 확인 불일치");
        }
        emailVerificationService.verify(email, VerificationPurpose.SIGNUP, code);
        User u = userService.createLocalUser(loginId, /* name */ loginId, email, password);
        return issueTokens(u, res);
    }

    public TokenBundle loginLocal(String loginId, String rawPassword, HttpServletResponse res) {
        User u = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인하세요."));
        if (u.getPassword() == null || !passwordEncoder.matches(rawPassword, u.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인하세요.");
        }
        return issueTokens(u, res);
    }

    public TokenBundle refresh(String refreshToken, HttpServletResponse res) {
        if (refreshToken == null
                || !tokenProvider.isValid(refreshToken)
                || !tokenProvider.isRefresh(refreshToken)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다.");
        }
        Long uid = tokenProvider.getUserId(refreshToken);
        User u = userService.getById(uid);
        return issueTokens(u, res);
    }

    public void logout(HttpServletResponse res) {
        CookieUtils.deleteCookie(res, refreshCookieName, refreshCookieSecure, refreshCookieDomain);
    }

    // ← 카카오 로그인에서도 쓸 수 있게 public 으로 변경
    public TokenBundle issueTokens(User u, HttpServletResponse res) {
        String access = tokenProvider.createAccessToken(u);
        String refresh = tokenProvider.createRefreshToken(u);
        CookieUtils.addHttpOnlyCookie(
                res,
                refreshCookieName,
                refresh,
                (int) java.time.Duration.ofDays(30).toSeconds(),
                refreshCookieSecure,
                refreshCookieDomain
        );
        return new TokenBundle(access, u);
    }

    public record TokenBundle(String accessToken, User user) {}
}
