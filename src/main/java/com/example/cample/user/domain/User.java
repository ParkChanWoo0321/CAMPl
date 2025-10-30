// src/main/java/com/example/cample/user/domain/User.java
package com.example.cample.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_login_id", columnNames = "login_id"),
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, length = 30)
    private String loginId;          // 로그인 ID

    @Column(nullable = false, length = 50)
    private String name;             // 실명

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String password;         // LOCAL만 사용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuthProvider provider;   // LOCAL / KAKAO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null) status = UserStatus.ACTIVE;
        if (provider == null) provider = AuthProvider.LOCAL; // 방어적 기본값
    }
}
