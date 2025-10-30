package com.example.cample.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity @Table(name="email_verification",
        indexes = { @Index(name="ix_ev_email_purpose", columnList = "email,purpose") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmailVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String email;

    @Column(nullable=false, length=10)
    private String code;

    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
    private VerificationPurpose purpose;

    @Column(nullable=false)
    private LocalDateTime expiresAt;

    private LocalDateTime verifiedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
