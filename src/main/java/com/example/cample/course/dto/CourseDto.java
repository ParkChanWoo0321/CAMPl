// src/main/java/com/example/cample/course/dto/CourseDto.java
package com.example.cample.course.dto;

import com.example.cample.course.domain.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseDto {
    private Long id;
    private String semesterCode;
    private String courseCode;
    private String name;
    private String professor;
    private String section;
    private Integer credit;
    private String year;
    private Long categoryId;
    private String categoryName;

    @Builder.Default
    private List<Slot> times = new ArrayList<>();

    private Double ratingAvg;
    private Long ratingCount;

    // 상세 전용: 강의평 목록
    @Builder.Default
    private List<ReviewResponse> reviews = new ArrayList<>();

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Slot {
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String room;
    }

    // 리스트/검색용(리뷰 없음)
    public static CourseDto from(Course c, List<CourseTime> timeList, Double avg, Long count) {
        return CourseDto.builder()
                .id(c.getId())
                .semesterCode(c.getSemesterCode())
                .courseCode(c.getCourseCode())
                .name(c.getName())
                .professor(c.getProfessor())
                .section(c.getSection())
                .credit(c.getCredit())
                .year(c.getYear())
                .categoryId(c.getCategory() != null ? c.getCategory().getId() : null)
                .categoryName(c.getCategory() != null ? c.getCategory().getName() : null)
                .times(timeList.stream().map(t -> Slot.builder()
                        .dayOfWeek(t.getDayOfWeek())
                        .startTime(t.getStartTime())
                        .endTime(t.getEndTime())
                        .room(t.getRoom())
                        .build()).collect(Collectors.toList()))
                .ratingAvg(avg)
                .ratingCount(count)
                .build();
    }

    // 단건 상세용(리뷰 포함)
    public static CourseDto fromDetailed(Course c, List<CourseTime> timeList, Double avg, Long count,
                                         List<ReviewResponse> reviewList) {
        CourseDto dto = from(c, timeList, avg, count);
        dto.setReviews(reviewList != null ? reviewList : List.of());
        return dto;
    }
}
