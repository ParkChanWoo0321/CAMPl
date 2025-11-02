package com.example.cample.course.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_review_course_user", columnNames = {"course_id","user_id"})
        },
        indexes = {
                @Index(name = "idx_review_course", columnList = "course_id"),
                @Index(name = "idx_review_user", columnList = "user_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseReview {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 내부 매핑(외부 노출은 익명)

    @Column(nullable = false)
    private Double rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
