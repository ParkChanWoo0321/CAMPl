// src/main/java/com/example/cample/security/oauth/OAuth2LoginSuccessHandler.java
package com.example.cample.security.oauth;

import com.example.cample.common.util.CookieUtils;
import com.example.cample.security.JwtTokenProvider;
import com.example.cample.user.domain.User;
import com.example.cample.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Map;

@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final String refreshCookieName;
    private final String refreshCookieDomain;
    private final boolean refreshCookieSecure;

    // 프론트 착지 경로 (원하는 경로로 바꿔도 됨)
    private static final String FRONTEND_REDIRECT = "http://localhost:3000/oauth/signed-in";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth) {
        try {
            OAuth2User oau = (OAuth2User) auth.getPrincipal();
            Map<String, Object> attrs = oau.getAttributes();

            String kakaoId = String.valueOf(attrs.get("id"));
            String email = KakaoOAuth2UserService.extractKakaoEmail(attrs);
            if (email == null) email = "kakao_" + kakaoId + "@kakao.local";

            String displayName = KakaoOAuth2UserService.extractKakaoName(attrs);
            if (displayName == null || displayName.isBlank()) displayName = "카카오사용자";

            User user = userService.upsertKakaoUser(kakaoId, email, displayName);

            // 리프레시 토큰만 쿠키에 심고
            String refresh = tokenProvider.createRefreshToken(user);
            CookieUtils.addHttpOnlyCookie(
                    res, refreshCookieName, refresh,
                    (int) java.time.Duration.ofDays(30).toSeconds(),
                    refreshCookieSecure, refreshCookieDomain
            );

            // 프론트로 리다이렉트 (바디/URL에 access 토큰 노출 없음)
            res.setStatus(HttpServletResponse.SC_FOUND); // 302
            res.setHeader("Location", FRONTEND_REDIRECT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
