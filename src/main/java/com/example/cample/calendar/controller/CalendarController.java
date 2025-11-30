// src/main/java/com/example/cample/calendar/controller/CalendarController.java
package com.example.cample.calendar.controller;

import com.example.cample.calendar.dto.CalendarEventDto;
import com.example.cample.calendar.service.CalendarService;
import com.example.cample.place.domain.Place;
import com.example.cample.place.domain.PlaceType; // ★ 추가
import com.example.cample.place.repo.PlaceRepository;
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
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService service;
    private final PlaceRepository placeRepository;

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

    // 홈 화면용 오늘 요약
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

        // PERSONAL + SCHOOL
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

        var placeMarkers = buildPlaceMarkers(items);
        var studyPlaces = service.getStudyPlaces(lat, lon);

        return Map.of(
                "date", target.toString(),
                "lectureCount", lectures.size(),
                "eventCount", events.size(),
                "lectures", lectures,
                "events", events,
                "ddays", ddays,
                "placeMarkerCount", placeMarkers.size(),
                "placeMarkers", placeMarkers,
                "studyPlaces", studyPlaces
        );
    }

    // 맵 페이지 전용 API (지도 마커 + 지난 일정/다음 일정 + 주변 시설 3곳 + 그 날 전체 강의/일정)
    @GetMapping("/map/{lat}/{lon}")
    public Map<String, Object> mapOverview(
            @AuthenticationPrincipal CustomUserPrincipal me,
            @PathVariable Double lat,
            @PathVariable Double lon,
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

        var dayEvents = items.stream()
                .filter(e -> e.getType() != null &&
                        (e.getType().name().equals("PERSONAL") || e.getType().name().equals("SCHOOL")))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("title", e.getTitle());
                    if (e.getLocation() != null && !e.getLocation().isBlank()) m.put("location", e.getLocation());
                    m.put("startAt", e.getStartAt());
                    m.put("endAt", e.getEndAt());
                    if (e.getCategory() != null) m.put("category", e.getCategory());
                    if (e.getImportant() != null) m.put("important", e.getImportant());
                    if (e.getType() != null) m.put("type", e.getType());
                    if (e.getOrigin() != null) m.put("origin", e.getOrigin());
                    return m;
                })
                .toList();

        var placeMarkers = buildPlaceMarkers(items);

        var pastEvents = new ArrayList<Map<String, Object>>();
        var upcomingEvents = new ArrayList<Map<String, Object>>();

        for (CalendarEventDto e : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("title", e.getTitle());
            if (e.getLocation() != null && !e.getLocation().isBlank()) {
                m.put("location", e.getLocation());
            }
            m.put("startAt", e.getStartAt());
            m.put("endAt", e.getEndAt());
            if (e.getCategory() != null) m.put("category", e.getCategory());
            if (e.getImportant() != null) m.put("important", e.getImportant());
            if (e.getType() != null) m.put("type", e.getType());
            if (e.getOrigin() != null) m.put("origin", e.getOrigin());

            if (!e.getEndAt().isAfter(pivot)) {
                pastEvents.add(m);
            } else {
                upcomingEvents.add(m);
            }
        }

        var nearbyPlaces = service.getNearbyPlaces(lat, lon, 3);

        return Map.of(
                "date", target.toString(),
                "placeMarkerCount", placeMarkers.size(),
                "placeMarkers", placeMarkers,
                "pastEvents", pastEvents,
                "upcomingEvents", upcomingEvents,
                "nearbyPlaces", nearbyPlaces,
                "lectures", lectures,
                "events", dayEvents
        );
    }

    // "[H01] 이학관 307" -> "[H01] 이학관"
    // "[H07] 연암도서관" / "[H7] 연암도서관" -> "[H07] 연암도서관"
    // "세이커피" -> "세이커피"
    private String extractMarkerKey(String location) {
        if (location == null) return "";
        String trimmed = location.trim();
        if (trimmed.isEmpty()) return "";

        trimmed = normalizeBuildingCode(trimmed);

        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) {
            return trimmed;
        }

        String last = parts[parts.length - 1];

        if (last.matches(".*\\d.*")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) sb.append(' ');
                sb.append(parts[i]);
            }
            return sb.toString().trim();
        }

        return trimmed;
    }

    // "[H7] 연암도서관" -> "[H07] 연암도서관"
    private String normalizeBuildingCode(String s) {
        int endBracket = s.indexOf(']');
        if (s.startsWith("[H") && endBracket > 2) {
            String num = s.substring(2, endBracket);
            if (num.length() == 1) {
                String padded = "[H0" + num + "]";
                return padded + s.substring(endBracket + 1);
            }
        }
        return s;
    }

    private List<Map<String, Object>> buildPlaceMarkers(List<CalendarEventDto> items) {

        // 위치가 있는 일정만
        var located = items.stream()
                .filter(e -> e.getLocation() != null && !e.getLocation().isBlank())
                .toList();
        if (located.isEmpty()) {
            return List.of();
        }

        // 캠퍼스 건물만 대상으로 (CAMPUS_BUILDING)
        List<Place> buildings = placeRepository.findByType(PlaceType.CAMPUS_BUILDING);
        if (buildings.isEmpty()) {
            return List.of();
        }

        // 이름 -> Place 매핑 (좌표 있는 것만)
        Map<String, Place> byName = buildings.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .collect(Collectors.toMap(Place::getName, p -> p));

        if (byName.isEmpty()) {
            return List.of();
        }

        // placeId 기준 집계: "[H01] 이학관" / "이학관" 모두 한 건물로 합치기
        Map<Long, Map<String, Object>> agg = new LinkedHashMap<>();

        for (CalendarEventDto e : located) {
            String loc = e.getLocation().trim();
            String base = extractMarkerKey(loc); // 예: "[H01] 이학관", "이학관"

            // 1차: 이름 완전 일치
            Place place = byName.get(base);

            // 2차: 끝부분 일치로 매칭 (Place="[H01] 이학관", base="이학관")
            if (place == null) {
                String keyForMatch = base;
                place = buildings.stream()
                        .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                        .filter(p -> p.getName().endsWith(keyForMatch))
                        .findFirst()
                        .orElse(null);
            }

            if (place == null) continue;

            Map<String, Object> m = agg.get(place.getId());
            if (m == null) {
                m = new LinkedHashMap<>();
                m.put("name", place.getName());          // 항상 Place 이름 사용
                m.put("latitude", place.getLatitude());
                m.put("longitude", place.getLongitude());
                m.put("count", 0);
                agg.put(place.getId(), m);
            }
            int cnt = (int) m.get("count");
            m.put("count", cnt + 1);
        }

        return new ArrayList<>(agg.values());
    }

}
