// src/main/java/com/example/cample/security/oauth/OAuth2LoginSuccessHandler.java
package com.example.cample.security.oauth;

import com.example.cample.auth.service.AuthService;
import com.example.cample.user.domain.User;
import com.example.cample.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Map;

@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final AuthService authService;
    private final String successRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication auth) throws IOException {

        OAuth2User oau = (OAuth2User) auth.getPrincipal();
        Map<String, Object> attrs = oau.getAttributes();

        String kakaoId = String.valueOf(attrs.get("id"));
        String email = KakaoOAuth2UserService.extractKakaoEmail(attrs);
        if (email == null) {
            email = "kakao_" + kakaoId + "@kakao.local";
        }

        String displayName = KakaoOAuth2UserService.extractKakaoName(attrs);
        if (displayName == null || displayName.isBlank()) {
            displayName = "카카오사용자";
        }

        User user = userService.upsertKakaoUser(kakaoId, email, displayName);

        // 로컬 로그인과 동일한 토큰/쿠키 발급 로직 사용
        authService.issueTokens(user, res);

        // 프론트로 리다이렉트
        res.sendRedirect(successRedirect);
    }
}
