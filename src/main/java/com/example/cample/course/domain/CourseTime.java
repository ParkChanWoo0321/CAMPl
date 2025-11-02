package com.example.cample.course.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "course_times",
        indexes = {
                @Index(name = "idx_ct_course", columnList = "course_id"),
                @Index(name = "idx_ct_day", columnList = "day_of_week")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 100)
    private String room;
}
