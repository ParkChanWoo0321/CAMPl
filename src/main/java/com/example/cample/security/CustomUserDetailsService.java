// src/main/java/com/example/cample/security/CustomUserDetailsService.java
package com.example.cample.security;

import com.example.cample.security.model.CustomUserPrincipal;
import com.example.cample.user.domain.User;
import com.example.cample.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        User u = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginId));
        return new CustomUserPrincipal(u);
    }
}
