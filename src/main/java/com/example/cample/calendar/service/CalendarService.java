// src/main/java/com/example/cample/calendar/service/CalendarService.java
package com.example.cample.calendar.service;

import com.example.cample.calendar.domain.CalendarEvent;
import com.example.cample.calendar.domain.EventType;
import com.example.cample.calendar.dto.CalendarEventDto;
import com.example.cample.calendar.repo.CalendarEventRepository;
import com.example.cample.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarEventRepository repo;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Transactional(readOnly = true)
    public List<CalendarEventDto> list(LocalDateTime from, LocalDateTime to, Long me) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "from/to 범위가 유효하지 않습니다");
        }
        return repo.findIntersect(from, to, me, EventType.SCHOOL, EventType.PERSONAL)
                .stream().map(CalendarEventDto::from).toList();
    }

    @Transactional
    public CalendarEventDto create(CalendarEventDto req, Long me) {
        validateUpsert(req);

        LocalDateTime[] win = normalize(req.getAllDay(), req.getStartAt(), req.getEndAt());
        CalendarEvent e = CalendarEvent.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .startAt(win[0])
                .endAt(win[1])
                .allDay(Boolean.TRUE.equals(req.getAllDay()))
                .type(EventType.PERSONAL) // 강제
                .ownerId(me)
                .location(req.getLocation())
                .build();

        return CalendarEventDto.from(repo.save(e));
    }

    @Transactional
    public CalendarEventDto update(Long id, CalendarEventDto req, Long me) {
        validateUpsert(req);

        CalendarEvent e = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이벤트 없음"));
        if (e.getType() == EventType.SCHOOL) {
            throw new ApiException(HttpStatus.FORBIDDEN, "학교 일정은 수정할 수 없습니다");
        }
        if (e.getOwnerId() == null || !e.getOwnerId().equals(me)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 일정만 수정할 수 있습니다");
        }

        LocalDateTime[] win = normalize(req.getAllDay(), req.getStartAt(), req.getEndAt());
        e.setTitle(req.getTitle());
        e.setDescription(req.getDescription());
        e.setStartAt(win[0]);
        e.setEndAt(win[1]);
        e.setAllDay(Boolean.TRUE.equals(req.getAllDay()));
        e.setLocation(req.getLocation());

        return CalendarEventDto.from(e);
    }

    @Transactional
    public void delete(Long id, Long me) {
        CalendarEvent e = repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이벤트 없음"));
        if (e.getType() == EventType.SCHOOL) {
            throw new ApiException(HttpStatus.FORBIDDEN, "학교 일정은 삭제할 수 없습니다");
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
    }

    // 종일: [해당일 00:00, 다음날 00:00)
    private LocalDateTime[] normalize(boolean allDay, LocalDateTime start, LocalDateTime end) {
        if (!allDay) return new LocalDateTime[]{start, end};
        LocalDate s = start.atZone(KST).toLocalDate();
        LocalDateTime s0 = s.atStartOfDay();
        LocalDateTime e0 = s.plusDays(1).atStartOfDay();
        return new LocalDateTime[]{s0, e0};
    }
}
