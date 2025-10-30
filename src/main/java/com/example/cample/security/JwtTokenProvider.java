package com.example.cample.security;

import com.example.cample.user.domain.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl:900s}") Duration accessTtl,
            @Value("${app.jwt.refresh-token-ttl:30d}") Duration refreshTtl
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtl.toSeconds();
        this.refreshTtlSeconds = refreshTtl.toSeconds();
    }

    public String createAccessToken(User u) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claims(Map.of(
                        "loginId", u.getLoginId(),
                        "name", u.getName(),
                        "email", u.getEmail(),
                        "provider", u.getProvider().name()
                ))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String createRefreshToken(User u) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("typ", "refresh")
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }

    public boolean isValid(String token) {
        try { parse(token); return true; } catch (Exception e) { return false; }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parse(token).getPayload().getSubject());
    }

    public boolean isRefresh(String token) {
        try { return "refresh".equals(parse(token).getPayload().get("typ")); }
        catch (Exception e) { return false; }
    }
}
