// src/main/java/com/example/cample/calendar/service/CalendarService.java
package com.example.cample.calendar.service;

import com.example.cample.calendar.domain.CalendarEvent;
import com.example.cample.calendar.domain.EventCategory;
import com.example.cample.calendar.domain.EventType;
import com.example.cample.calendar.dto.CalendarEventDto;
import com.example.cample.calendar.repo.CalendarEventRepository;
import com.example.cample.common.exception.ApiException;
import com.example.cample.place.domain.PlaceType;
import com.example.cample.place.dto.PlaceSummaryDto;
import com.example.cample.place.repo.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarEventRepository repo;
    private final PlaceRepository placeRepository;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Transactional(readOnly = true)
    public List<CalendarEventDto> list(LocalDateTime from, LocalDateTime to, Long me) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "from/to 범위가 유효하지 않습니다");
        }
        return repo.findIntersectWithOwnerTypes(
                        from, to, me,
                        EventType.SCHOOL,
                        List.of(EventType.PERSONAL, EventType.LECTURE)
                )
                .stream()
                .map(CalendarEventDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDto> listByYearMonth(String ym, Long me) {
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(ym); // yyyy-MM
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ym 형식은 yyyy-MM 이어야 합니다");
        }
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        return list(from, to, me);
    }

    @Transactional
    public CalendarEventDto create(CalendarEventDto req, Long me) {
        validateUpsert(req);

        CalendarEvent e = CalendarEvent.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .startAt(req.getStartAt())
                .endAt(req.getEndAt())
                .type(EventType.PERSONAL)
                .ownerId(me)
                .location(req.getLocation())
                .category(req.getCategory())
                .important(Boolean.TRUE.equals(req.getImportant()))
                .build();

        return CalendarEventDto.from(repo.save(e));
    }

    @Transactional
    public CalendarEventDto update(Long id, CalendarEventDto req, Long me) {
        validateUpsert(req);

        CalendarEvent e = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이벤트 없음"));

        if (e.getType() == EventType.SCHOOL || e.getType() == EventType.LECTURE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "학교/강의 연동 일정은 수정할 수 없습니다");
        }
        if (e.getOwnerId() == null || !e.getOwnerId().equals(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 일정만 수정할 수 있습니다");
        }

        e.setTitle(req.getTitle());
        e.setDescription(req.getDescription());
        e.setStartAt(req.getStartAt());
        e.setEndAt(req.getEndAt());
        e.setLocation(req.getLocation());
        e.setCategory(req.getCategory());
        e.setImportant(Boolean.TRUE.equals(req.getImportant()));

        return CalendarEventDto.from(e);
    }

    @Transactional
    public void delete(Long id, Long me) {
        CalendarEvent e = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이벤트 없음"));

        if (e.getType() == EventType.SCHOOL || e.getType() == EventType.LECTURE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "학교/강의 연동 일정은 삭제할 수 없습니다");
        }
        if (e.getOwnerId() == null || !e.getOwnerId().equals(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 일정만 삭제할 수 있습니다");
        }
        repo.delete(e);
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDto> listToday(Long me) {
        LocalDate today = LocalDate.now(KST);
        LocalDateTime from = today.atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();
        return list(from, to, me);
    }

    @Transactional(readOnly = true)
    public List<CalendarEventDto> importantUpcoming(Long me, LocalDateTime now) {
        return repo.findImportantUpcoming(me, EventType.PERSONAL, now)
                .stream().map(CalendarEventDto::from).toList();
    }

    private void validateUpsert(CalendarEventDto req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "제목은 필수입니다");
        }
        if (req.getStartAt() == null || req.getEndAt() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "시작/종료 시각은 필수입니다");
        }
        if (!req.getStartAt().isBefore(req.getEndAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startAt < endAt 이어야 합니다");
        }
        if (req.getCategory() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "카테고리는 필수입니다");
        }
        if (req.getImportant() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "important는 필수입니다");
        }
    }

    // ===== 시간표 연동 헬퍼 =====

    @Transactional
    public Long createLectureEvent(String title, String location,
                                   LocalDateTime start, LocalDateTime end, Long ownerId) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startAt < endAt 이어야 합니다");
        }
        CalendarEvent e = CalendarEvent.builder()
                .title(title)
                .description(null)
                .startAt(start)
                .endAt(end)
                .type(EventType.LECTURE)
                .ownerId(ownerId)
                .location(location)
                .category(EventCategory.LECTURE)
                .important(false)
                .build();
        return repo.save(e).getId();
    }

    @Transactional
    public void deleteEventsByIdsForOwner(List<Long> ids, Long ownerId) {
        if (ids == null || ids.isEmpty()) return;
        var events = repo.findAllById(ids);
        boolean illegal = events.stream()
                .anyMatch(e -> e.getOwnerId() == null || !e.getOwnerId().equals(ownerId));
        if (illegal) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 일정만 삭제할 수 있습니다");
        }
        repo.deleteAllByIdInBatch(ids);
    }

    // ===== 메인 화면용 카페 추천 =====

    @Transactional(readOnly = true)
    public List<PlaceSummaryDto> getStudyPlaces(Double baseLat, Double baseLon) {
        var cafes = placeRepository.findByType(PlaceType.CAFE);
        if (cafes.isEmpty()) {
            return List.of();
        }

        Collections.shuffle(cafes); // 랜덤 정렬
        return cafes.stream()
                .limit(2)
                .map(p -> {
                    Integer distance = calcDistanceMeters(
                            baseLat, baseLon,
                            p.getLatitude(), p.getLongitude()
                    );
                    return PlaceSummaryDto.from(p, distance);
                })
                .toList();
    }

    private Integer calcDistanceMeters(Double baseLat, Double baseLon,
                                       Double targetLat, Double targetLon) {
        if (baseLat == null || baseLon == null ||
                targetLat == null || targetLon == null) {
            return null; // 위치 정보 없으면 거리 계산 안 함
        }

        double R = 6371000.0; // 지구 반지름 (m)
        double dLat = Math.toRadians(targetLat - baseLat);
        double dLon = Math.toRadians(targetLon - baseLon);
        double rLat1 = Math.toRadians(baseLat);
        double rLat2 = Math.toRadians(targetLat);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) Math.round(R * c);
    }
}
