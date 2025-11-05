// src/main/java/com/example/cample/calendar/domain/CalendarEvent.java
package com.example.cample.calendar.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "calendar_events",
        indexes = {
                @Index(name = "idx_event_range", columnList = "startAt,endAt"),
                @Index(name = "idx_owner_start", columnList = "ownerId,startAt"),
                @Index(name = "idx_type_start", columnList = "type,startAt")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CalendarEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventType type; // SCHOOL / PERSONAL / LECTURE

    // PERSONAL/LECTURE일 때 소유자, SCHOOL은 NULL
    private Long ownerId;

    @Column(length = 100)
    private String location;

    // ✅ 추가: 카테고리(강의/발표/팀플/모임)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventCategory category;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
