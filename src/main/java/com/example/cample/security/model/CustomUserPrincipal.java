// src/main/java/com/example/cample/security/model/CustomUserPrincipal.java
package com.example.cample.security.model;

import com.example.cample.user.domain.User;
import com.example.cample.user.domain.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserPrincipal implements UserDetails {
    private final User user;

    public CustomUserPrincipal(User user) { this.user = user; }

    public Long getId() { return user.getId(); }
    public String getEmail() { return user.getEmail(); }
    public String getName() { return user.getName(); }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public String getPassword() { return user.getPassword() == null ? "" : user.getPassword(); }
    @Override public String getUsername() { return user.getLoginId(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return user.getStatus() != UserStatus.DELETED; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return user.getStatus() == UserStatus.ACTIVE; }
}
