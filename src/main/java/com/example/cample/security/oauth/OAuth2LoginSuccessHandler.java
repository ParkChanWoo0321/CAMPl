// src/main/java/com/example/cample/security/oauth/OAuth2LoginSuccessHandler.java
package com.example.cample.security.oauth;

import com.example.cample.common.util.CookieUtils;
import com.example.cample.security.JwtTokenProvider;
import com.example.cample.user.domain.User;
import com.example.cample.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;
    private final String refreshCookieName;
    private final String refreshCookieDomain;
    private final boolean refreshCookieSecure;

    private final ObjectMapper om = new ObjectMapper();

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

            String access = tokenProvider.createAccessToken(user);
            String refresh = tokenProvider.createRefreshToken(user);

            CookieUtils.addHttpOnlyCookie(
                    res, refreshCookieName, refresh,
                    (int) java.time.Duration.ofDays(30).toSeconds(),
                    refreshCookieSecure, refreshCookieDomain
            );

            res.setStatus(200);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            om.writeValue(res.getWriter(), Map.of(
                    "accessToken", access,
                    "id", user.getId(),
                    "loginId", user.getLoginId(),
                    "name", user.getName(),
                    "email", user.getEmail(),
                    "provider", user.getProvider().name()
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
