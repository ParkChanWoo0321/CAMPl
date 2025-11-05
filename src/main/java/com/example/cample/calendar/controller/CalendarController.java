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

    // ✅ 새 API: 캘린더에서 특정 '날짜'를 선택했을 때 그 날의 일정만 그대로 반환
    @GetMapping("/day")
    public List<CalendarEventDto> day(
            @AuthenticationPrincipal CustomUserPrincipal me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return service.list(from, to, me.getId());
    }

    // ✅ 확장된 "오늘 안내" (또는 ?date=yyyy-MM-dd 테스트)
    //    요구대로 items 제거, 장소별 개수 + 과거/다음(카테고리/제목만)만 응답
    @GetMapping("/summary/today")
    public Map<String, Object> summaryToday(
            @AuthenticationPrincipal CustomUserPrincipal me,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date // yyyy-MM-dd (옵션)
    ) {
        ZoneId KST = ZoneId.of("Asia/Seoul");
        LocalDate target = (date != null) ? date : LocalDate.now(KST);
        LocalDateTime from = target.atStartOfDay();
        LocalDateTime to = target.plusDays(1).atStartOfDay();

        var items = service.list(from, to, me.getId());

        LocalDateTime now = LocalDateTime.now(KST);
        var past = items.stream()
                .filter(e -> !e.getEndAt().isAfter(now)) // endAt <= now
                .map(e -> Map.of("category", e.getCategory(), "title", e.getTitle()))
                .toList();

        var upcoming = items.stream()
                .filter(e -> e.getStartAt().isAfter(now)) // startAt > now
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
