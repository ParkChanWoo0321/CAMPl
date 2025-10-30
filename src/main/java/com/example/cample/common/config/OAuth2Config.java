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
    @Value("${app.security.refresh-cookie-domain:localhost}")
    private String refreshCookieDomain;
    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Bean
    public OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler(
            JwtTokenProvider tokenProvider,
            UserService userService
    ) {
        return new OAuth2LoginSuccessHandler(tokenProvider, userService,
                refreshCookieName, refreshCookieDomain, refreshCookieSecure);
    }
}
