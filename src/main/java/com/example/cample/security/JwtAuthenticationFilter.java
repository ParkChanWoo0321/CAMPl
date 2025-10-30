// src/main/java/com/example/cample/security/JwtAuthenticationFilter.java
package com.example.cample.security;

import com.example.cample.security.model.CustomUserPrincipal;
import com.example.cample.user.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        // 이미 인증된 경우 재설정하지 않음
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String header = req.getHeader("Authorization");

            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                if (tokenProvider.isValid(token) && !tokenProvider.isRefresh(token)) {
                    Long userId = tokenProvider.getUserId(token);

                    if (userId != null) {
                        userRepository.findById(userId).ifPresent(user -> {
                            // ★ principal을 반드시 CustomUserPrincipal로 설정
                            CustomUserPrincipal principal = new CustomUserPrincipal(user);

                            var authorities = principal.getAuthorities();
                            if (authorities == null) authorities = Collections.emptyList();

                            var authentication = new UsernamePasswordAuthenticationToken(
                                    principal, null, authorities
                            );
                            authentication.setDetails(
                                    new WebAuthenticationDetailsSource().buildDetails(req)
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        });
                    }
                }
            }
        }

        chain.doFilter(req, res);
    }
}
