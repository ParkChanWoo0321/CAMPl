// src/main/java/com/example/cample/calendar/dto/CalendarEventDto.java
package com.example.cample.calendar.dto;

import com.example.cample.calendar.domain.CalendarEvent;
import com.example.cample.calendar.domain.EventCategory;
import com.example.cample.calendar.domain.EventType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CalendarEventDto {

    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title;

    private String description;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    @Size(max = 100)
    private String location;

    // 응답에 포함(SCHOOL/PERSONAL/LECTURE), 요청 시 무시됨
    private EventType type;

    // ✅ 추가: 요청/응답에 모두 포함 (강의/발표/팀플/모임)
    @NotNull
    private EventCategory category;

    public static CalendarEventDto from(CalendarEvent e) {
        return CalendarEventDto.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .startAt(e.getStartAt())
                .endAt(e.getEndAt())
                .location(e.getLocation())
                .type(e.getType())
                .category(e.getCategory())
                .build();
    }
}
