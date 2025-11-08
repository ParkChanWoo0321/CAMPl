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

    // 프로퍼티로 주입(예: app.oauth.success-redirect)
    private final String successRedirect;

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

            // refresh 토큰을 HTTP-Only 쿠키로 설정
            String refresh = tokenProvider.createRefreshToken(user);
            CookieUtils.addHttpOnlyCookie(
                    res, refreshCookieName, refresh,
                    (int) java.time.Duration.ofDays(30).toSeconds(),
                    refreshCookieSecure, refreshCookieDomain
            );

            // 설정값으로 리다이렉트
            res.sendRedirect(successRedirect);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
