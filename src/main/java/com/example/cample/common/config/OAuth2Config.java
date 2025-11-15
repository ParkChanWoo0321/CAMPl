// src/main/java/com/example/cample/common/config/OAuth2Config.java
package com.example.cample.common.config;

import com.example.cample.auth.service.AuthService;
import com.example.cample.security.oauth.OAuth2LoginSuccessHandler;
import com.example.cample.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuth2Config {

    // 성공 리다이렉트 외부화
    @Value("${app.oauth.success-redirect:http://localhost:3000/oauth/signed-in}")
    private String successRedirect;

    @Bean
    public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(
            UserService userService,
            AuthService authService
    ) {
        return new OAuth2LoginSuccessHandler(
                userService,
                authService,
                successRedirect
        );
    }
}
