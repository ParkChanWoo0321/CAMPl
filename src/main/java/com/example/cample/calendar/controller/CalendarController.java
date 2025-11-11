// src/main/java/com/example/cample/calendar/controller/CalendarController.java
package com.example.cample.calendar.controller;

import com.example.cample.calendar.dto.CalendarEventDto;
import com.example.cample.calendar.service.CalendarService;
import com.example.cample.security.model.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService service;

    @GetMapping(value = "/events", params = {"from","to"})
    public List<CalendarEventDto> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal CustomUserPrincipal me
    ) {
        return service.list(from, to, me.getId());
    }

    @GetMapping(value = "/events", params = "ym")
    public List<CalendarEventDto> listByMonth(
            @RequestParam String ym,
            @AuthenticationPrincipal CustomUserPrincipal me
    ) {
        return service.listByYearMonth(ym, me.getId());
    }

    @PostMapping("/events")
    public CalendarEventDto create(
            @Valid @RequestBody CalendarEventDto req,
            @AuthenticationPrincipal CustomUserPrincipal me
    ) {
        return service.create(req, me.getId());
    }

    @PutMapping("/events/{id}")
    public CalendarEventDto update(
            @PathVariable Long id,
            @Valid @RequestBody CalendarEventDto req,
            @AuthenticationPrincipal CustomUserPrincipal me
    ) {
        return service.update(id, req, me.getId());
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal me
    ) {
        service.delete(id, me.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/day")
    public List<CalendarEventDto> day(
            @AuthenticationPrincipal CustomUserPrincipal me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return service.list(from, to, me.getId());
    }

    // lectures / events / ddays
    @GetMapping("/summary/today")
    public Map<String, Object> summaryToday(
            @AuthenticationPrincipal CustomUserPrincipal me,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false, name = "asOf")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime asOf
    ) {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = (date != null) ? date : LocalDate.now(KST);
        LocalDateTime from = target.atStartOfDay();
        LocalDateTime to = target.plusDays(1).atStartOfDay();

        LocalDateTime pivot = (asOf != null)
                ? asOf
                : (date != null ? LocalDateTime.of(target, LocalTime.now(KST)) : LocalDateTime.now(KST));

        var items = service.list(from, to, me.getId());

        var lectures = items.stream()
                .filter(e -> e.getType() != null && e.getType().name().equals("LECTURE"))
                .map(e -> Map.of(
                        "courseName", e.getTitle(),
                        "location", e.getLocation(),
                        "dayOfWeek", e.getStartAt().getDayOfWeek().name(),
                        "startAt", e.getStartAt(),
                        "endAt", e.getEndAt()
                ))
                .toList();

        // ✅ SCHOOL 포함 + origin 제공
        var events = items.stream()
                .filter(e -> e.getType() != null &&
                        (e.getType().name().equals("PERSONAL") || e.getType().name().equals("SCHOOL")))
                .map(e -> Map.of(
                        "title", e.getTitle(),
                        "location", e.getLocation(),
                        "startAt", e.getStartAt(),
                        "endAt", e.getEndAt(),
                        "category", e.getCategory(),
                        "important", e.getImportant(),
                        "origin", e.getType().name().equals("SCHOOL") ? "SCHOOL" : "MANUAL"
                ))
                .toList();

        var importantUpcoming = service.importantUpcoming(me.getId(), pivot);
        var ddays = importantUpcoming.stream()
                .map(e -> {
                    long d = ChronoUnit.DAYS.between(pivot.toLocalDate(), e.getStartAt().toLocalDate());
                    String label = (d == 0) ? "D-DAY" : "D-" + d;
                    return Map.of(
                            "title", e.getTitle(),
                            "targetDate", e.getStartAt().toLocalDate().toString(),
                            "dDay", label
                    );
                })
                .toList();

        return Map.of(
                "date", target.toString(),
                "lectureCount", lectures.size(),
                "eventCount", events.size(),
                "lectures", lectures,
                "events", events,
                "ddays", ddays
        );
    }
}
