// src/main/java/com/example/cample/security/JwtAuthenticationFilter.java
package com.example.cample.security;

import com.example.cample.security.model.CustomUserPrincipal;
import com.example.cample.user.domain.UserStatus;
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

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String header = req.getHeader("Authorization");

            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                if (tokenProvider.isValid(token) && !tokenProvider.isRefresh(token)) {
                    Long userId = tokenProvider.getUserId(token);

                    if (userId != null) {
                        userRepository.findById(userId).ifPresent(user -> {
                            if (user.getStatus() != UserStatus.ACTIVE) {
                                return; // 비활성/탈퇴 계정은 인증 세팅 금지
                            }
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
