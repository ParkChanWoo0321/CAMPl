package com.example.cample.timetable.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConflictDto {
    private Long existingItemId;
    private Long existingCourseId;
    private String existingCourseName;

    private String day;           // 예: "MON"
    private String existing;      // "13:00-15:00"
    private String requested;     // "14:00-16:00"
}
