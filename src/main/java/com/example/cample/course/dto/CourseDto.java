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
    private String year;          // ← 추가: 학년
    private Long categoryId;
    private String categoryName;

    @Builder.Default
    private List<Slot> times = new ArrayList<>();

    private Double ratingAvg; // optional
    private Long ratingCount; // optional

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Slot {
        private DayOfWeek dayOfWeek;   // 요일
        private LocalTime startTime;   // 시작시간
        private LocalTime endTime;     // 종료시간
        private String room;           // 강의실
    }

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
}
