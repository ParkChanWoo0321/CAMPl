// src/main/java/com/example/cample/calendar/controller/CalendarController.java
package com.example.cample.calendar.controller;

import com.example.cample.calendar.dto.CalendarEventDto;
import com.example.cample.calendar.service.CalendarService;
import com.example.cample.place.domain.Place;
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

    // 홈 화면용 오늘 요약(기존) - 그대로 사용
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

        // 오늘 일정 기준 장소별 마커 계산
        var placeMarkers = buildPlaceMarkers(items);

        // 메인 화면 하단 "과제하기 좋아요!" 카페 2개
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

    // 맵 페이지 전용 API (지도 마커 + 지난 일정/다음 일정 + 주변 시설 3곳)
    @GetMapping("/map")
    public Map<String, Object> mapOverview(
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

        // 지도 마커용 집계
        var placeMarkers = buildPlaceMarkers(items);

        // 지난 일정 / 다음 일정 분리
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
            if (e.getType() != null) m.put("type", e.getType());
            if (e.getOrigin() != null) m.put("origin", e.getOrigin());

            // 종료 시각이 pivot 이전/같으면 지난 일정, 이후면 다음 일정
            if (!e.getEndAt().isAfter(pivot)) {
                pastEvents.add(m);
            } else {
                upcomingEvents.add(m);
            }
        }

        // 주변 시설 3곳 (현재 위치 기준)
        var nearbyPlaces = service.getNearbyPlaces(lat, lon, 3);

        return Map.of(
                "date", target.toString(),
                "placeMarkerCount", placeMarkers.size(),
                "placeMarkers", placeMarkers,
                "pastEvents", pastEvents,
                "upcomingEvents", upcomingEvents,
                "nearbyPlaces", nearbyPlaces
        );
    }

    // "[H01] 이학관 307" -> "[H01] 이학관"
    // "[H07] 연암도서관" / "[H7] 연암도서관" -> "[H07] 연암도서관"
    // "세이커피" -> "세이커피"
    private String extractMarkerKey(String location) {
        if (location == null) return "";
        String trimmed = location.trim();
        if (trimmed.isEmpty()) return "";

        // [H7] -> [H07] 같이 한 자리수를 두 자리로 보정
        trimmed = normalizeBuildingCode(trimmed);

        // 공백 기준으로 분리
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) {
            // 단어 한 개면 그대로 (건물만 있는 경우)
            return trimmed;
        }

        String last = parts[parts.length - 1];

        // 마지막 토큰에 숫자가 들어 있으면 "호수/번호"라고 보고 제거
        if (last.matches(".*\\d.*")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) {
                if (i > 0) sb.append(' ');
                sb.append(parts[i]);
            }
            return sb.toString().trim();
        }

        // 마지막 토큰이 숫자 없으면 전체를 건물 이름으로 사용
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
        // 1) 오늘 일정에 등장하는 location → markerKey 세트
        Set<String> markerKeys = items.stream()
                .map(CalendarEventDto::getLocation)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::extractMarkerKey)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (markerKeys.isEmpty()) {
            return List.of();
        }

        // 2) Place 에서 이름으로 조회
        List<Place> places = placeRepository.findByNameIn(markerKeys);
        if (places.isEmpty()) {
            return List.of();
        }

        Map<String, Place> placeMap = places.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .collect(Collectors.toMap(
                        Place::getName,
                        p -> p
                ));

        if (placeMap.isEmpty()) {
            return List.of();
        }

        // 3) 장소별 일정 개수 집계
        Map<String, Map<String, Object>> agg = new LinkedHashMap<>();

        for (CalendarEventDto e : items) {
            String loc = e.getLocation();
            if (loc == null || loc.isBlank()) continue;

            String key = extractMarkerKey(loc.trim());
            Place place = placeMap.get(key);
            if (place == null) continue;

            Map<String, Object> m = agg.get(key);
            if (m == null) {
                m = new LinkedHashMap<>();
                m.put("name", key);
                m.put("latitude", place.getLatitude());
                m.put("longitude", place.getLongitude());
                m.put("count", 0);
                agg.put(key, m);
            }
            int cnt = (int) m.get("count");
            m.put("count", cnt + 1);
        }

        return new ArrayList<>(agg.values());
    }
}
