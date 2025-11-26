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

    // lectures / events / ddays + studyPlaces(카페 2개)
    @GetMapping("/summary/today")
    public Map<String, Object> summaryToday(
            @AuthenticationPrincipal CustomUserPrincipal me,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(required = false, name = "asOf")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime asOf,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon
    ) {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = (date != null) ? date : LocalDate.now(KST);
        LocalDateTime from = target.atStartOfDay();
        LocalDateTime to = target.plusDays(1).atStartOfDay();

        LocalDateTime pivot = (asOf != null)
                ? asOf
                : (date != null ? LocalDateTime.of(target, LocalTime.now(KST)) : LocalDateTime.now(KST));

        var items = service.list(from, to, me.getId());

        // LECTURE
        var lectures = items.stream()
                .filter(e -> e.getType() != null && e.getType().name().equals("LECTURE"))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("courseName", e.getTitle());
                    if (e.getLocation() != null && !e.getLocation().isBlank()) m.put("location", e.getLocation());
                    m.put("dayOfWeek", e.getStartAt().getDayOfWeek().name());
                    m.put("startAt", e.getStartAt());
                    m.put("endAt", e.getEndAt());
                    return m;
                })
                .toList();

        // PERSONAL + SCHOOL (null 안전)
        var events = items.stream()
                .filter(e -> e.getType() != null &&
                        (e.getType().name().equals("PERSONAL") || e.getType().name().equals("SCHOOL")))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("title", e.getTitle());
                    if (e.getLocation() != null && !e.getLocation().isBlank()) m.put("location", e.getLocation());
                    m.put("startAt", e.getStartAt());
                    m.put("endAt", e.getEndAt());
                    if (e.getCategory() != null) m.put("category", e.getCategory());
                    if (e.getImportant() != null) m.put("important", e.getImportant());
                    m.put("origin", e.getType().name().equals("SCHOOL") ? "SCHOOL" : "MANUAL");
                    return m;
                })
                .toList();

        var importantUpcoming = service.importantUpcoming(me.getId(), pivot);
        var ddays = importantUpcoming.stream()
                .map(e -> {
                    long d = ChronoUnit.DAYS.between(pivot.toLocalDate(), e.getStartAt().toLocalDate());
                    String label = (d == 0) ? "D-DAY" : "D-" + d;
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("title", e.getTitle());
                    m.put("targetDate", e.getStartAt().toLocalDate().toString());
                    m.put("dDay", label);
                    return m;
                })
                .toList();

        // 카페 2개 추천 + 거리 계산
        var studyPlaces = service.getStudyPlaces(lat, lon);

        return Map.of(
                "date", target.toString(),
                "lectureCount", lectures.size(),
                "eventCount", events.size(),
                "lectures", lectures,
                "events", events,
                "ddays", ddays,
                "studyPlaces", studyPlaces
        );
    }
}
