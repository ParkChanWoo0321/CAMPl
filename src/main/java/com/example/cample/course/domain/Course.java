// src/main/java/com/example/cample/course/domain/Course.java
package com.example.cample.course.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_course_semester_code_section",
                        columnNames = {"semester_code","course_code","section"})
        },
        indexes = {
                @Index(name = "idx_course_semester", columnList = "semester_code"),
                @Index(name = "idx_course_professor", columnList = "professor"),
                @Index(name = "idx_course_category", columnList = "category_id"),
                @Index(name = "idx_course_year", columnList = "year")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semester_code", nullable = false, length = 16)
    private String semesterCode; // "2025-2" 고정

    @Column(name = "course_code", nullable = false, length = 50)
    private String courseCode;   // 학수번호

    @Column(nullable = false, length = 200)
    private String name;         // 교과목명

    @Column(length = 100)
    private String professor;    // 담당교수명

    @Column(length = 10)
    private String section;      // 분반(예: "01")

    @Column
    private Integer credit;      // 학점

    @Column(name = "year")       // 학년 (예: 1,2,3,4)
    private String year;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CourseCategory category;

    @Column(name = "target_department", length = 100)
    private String targetDepartment; // "모든학과", "항공교통물류학과" 등
}
