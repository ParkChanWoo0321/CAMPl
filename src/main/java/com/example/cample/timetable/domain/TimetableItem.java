package com.example.cample.timetable.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "timetable_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_tt_user_sem_course",
                        columnNames = {"user_id", "semester_code", "course_id"})
        },
        indexes = {
                @Index(name = "idx_tt_user_semester", columnList = "user_id,semester_code")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimetableItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Column(name="semester_code", nullable = false, length = 16)
    private String semesterCode; // 2025-2

    @Column(name="course_id", nullable = false)
    private Long courseId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
