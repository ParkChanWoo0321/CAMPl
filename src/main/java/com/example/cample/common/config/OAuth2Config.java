// src/main/java/com/example/cample/common/config/OAuth2Config.java
package com.example.cample.common.config;

import com.example.cample.security.JwtTokenProvider;
import com.example.cample.security.oauth.OAuth2LoginSuccessHandler;
import com.example.cample.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuth2Config {

    @Value("${app.security.refresh-cookie-name:REFRESH_TOKEN}")
    private String refreshCookieName;

    // 공인 IP 테스트 시 도메인 비움
    @Value("${app.security.refresh-cookie-domain:}")
    private String refreshCookieDomain;

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    // 성공 리다이렉트 외부화
    @Value("${app.oauth.success-redirect:http://localhost:3000/oauth/signed-in}")
    private String successRedirect;

    @Bean
    public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(
            JwtTokenProvider tokenProvider,
            UserService userService
    ) {
        return new OAuth2LoginSuccessHandler(
                tokenProvider,
                userService,
                refreshCookieName,
                refreshCookieDomain,
                refreshCookieSecure,
                successRedirect
        );
    }
}
