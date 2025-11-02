package com.example.cample.course.dto;

import com.example.cample.course.domain.CourseReview;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewResponse {
    private Long id;
    private Double rating;
    private String content;
    private LocalDateTime createdAt;

    public static ReviewResponse from(CourseReview r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .content(r.getContent())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
