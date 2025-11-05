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
import java.time.LocalTime;     // ✅ 추가
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService service;

    @GetMapping("/events")
    public List<CalendarEventDto> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @AuthenticationPrincipal CustomUserPrincipal me
    ) {
        return service.list(from, to, me.getId());
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

    // 날짜 선택 시 그 날의 일정 원본 리스트
    @GetMapping("/day")
    public List<CalendarEventDto> day(
            @AuthenticationPrincipal CustomUserPrincipal me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return service.list(from, to, me.getId());
    }

    // 오늘(또는 특정 날짜)의 요약: 장소 집계 + 과거/다음(제목/카테고리만)
    // asOf를 주면 그 시각을 분기점으로 시뮬레이션, 없으면 실시간 분기
    @GetMapping("/summary/today")
    public Map<String, Object> summaryToday(
            @AuthenticationPrincipal CustomUserPrincipal me,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date, // yyyy-MM-dd (옵션)

            @RequestParam(required = false, name = "asOf")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime asOf // 예: 2025-11-05T13:00:00 (옵션)
    ) {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = (date != null) ? date : LocalDate.now(KST);
        LocalDateTime from = target.atStartOfDay();
        LocalDateTime to = target.plusDays(1).atStartOfDay();

        // ✅ 분기 시각(pivot) 결정
        LocalDateTime pivot = (asOf != null)
                ? asOf
                : (date != null
                ? LocalDateTime.of(target, LocalTime.now(KST))
                : LocalDateTime.now(KST));

        var items = service.list(from, to, me.getId());

        // ✅ 분류 규칙: past = endAt <= pivot, upcoming = endAt > pivot (진행중 포함)
        var past = items.stream()
                .filter(e -> !e.getEndAt().isAfter(pivot))
                .map(e -> Map.of("category", e.getCategory(), "title", e.getTitle()))
                .toList();

        var upcoming = items.stream()
                .filter(e -> e.getEndAt().isAfter(pivot))
                .map(e -> Map.of("category", e.getCategory(), "title", e.getTitle()))
                .toList();

        var locationCounts = items.stream()
                .map(CalendarEventDto::getLocation)
                .filter(loc -> loc != null && !loc.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> Map.of("location", e.getKey(), "count", e.getValue()))
                .toList();

        return Map.of(
                "date", target.toString(),
                "count", items.size(),
                "locationCounts", locationCounts,
                "past", past,
                "upcoming", upcoming
        );
    }
}
