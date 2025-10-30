// src/main/java/com/example/cample/security/oauth/KakaoOAuth2UserService.java
package com.example.cample.security.oauth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KakaoOAuth2UserService extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) {
        return super.loadUser(req);
    }

    @SuppressWarnings("unchecked")
    public static String extractKakaoEmail(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account != null) {
            Object email = account.get("email");
            if (email != null) return email.toString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String extractKakaoName(Map<String, Object> attributes) {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account != null) {
            Object name = account.get("name");
            if (name != null) return name.toString();
            Map<String, Object> profile = (Map<String, Object>) account.get("profile");
            if (profile != null) {
                Object nick = profile.get("nickname");
                if (nick != null) return nick.toString();
            }
        }
        Map<String, Object> props = (Map<String, Object>) attributes.get("properties");
        if (props != null) {
            Object nick = props.get("nickname");
            if (nick != null) return nick.toString();
        }
        return null;
    }
}
