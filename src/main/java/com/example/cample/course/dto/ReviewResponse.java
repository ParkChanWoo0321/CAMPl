package com.example.cample.course.dto;

import com.example.cample.course.domain.CourseReview;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;
    private Double rating;
    private String content;
    private LocalDateTime createdAt;
    private boolean mine;   // 내가 쓴 리뷰인지 여부

    // 기본용(기존 호출부 호환) - mine=false
    public static ReviewResponse from(CourseReview r) {
        return from(r, null);
    }

    // 로그인 유저 기준으로 mine 판별
    public static ReviewResponse from(CourseReview r, Long meId) {
        boolean isMine = (meId != null && meId.equals(r.getUserId()));
        return ReviewResponse.builder()
                .id(r.getId())
                .rating(r.getRating())
                .content(r.getContent())
                .createdAt(r.getCreatedAt())
                .mine(isMine)
                .build();
    }
}
