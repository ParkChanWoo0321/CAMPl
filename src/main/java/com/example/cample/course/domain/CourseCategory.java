package com.example.cample.course.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id")
    private CourseCategory parent;

    @OneToMany(mappedBy = "parent")
    @Builder.Default
    private List<CourseCategory> children = new ArrayList<>();
}
