// src/main/java/com/example/cample/timetable/service/TimetableService.java
package com.example.cample.timetable.service;

import com.example.cample.calendar.service.CalendarService;
import com.example.cample.common.constant.SemesterConst;
import com.example.cample.common.exception.ApiException;
import com.example.cample.course.domain.Course;
import com.example.cample.course.domain.CourseTime;
import com.example.cample.course.dto.CourseDto;
import com.example.cample.course.repo.CourseRepository;
import com.example.cample.course.repo.CourseTimeRepository;
import com.example.cample.timetable.domain.*;
import com.example.cample.timetable.dto.*;
import com.example.cample.timetable.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableItemRepository itemRepo;
    private final TimetableCalendarMapRepository mapRepo;
    private final CourseRepository courseRepo;
    private final CourseTimeRepository timeRepo;
    private final CalendarService calendarService;

    // 기존: courseId 목록만 리턴
    @Transactional(readOnly = true)
    public List<Long> myCourseIds(Long userId) {
        return itemRepo.findByUserIdAndSemesterCode(userId, SemesterConst.SEMESTER_CODE)
                .stream().map(TimetableItem::getCourseId).toList();
    }

    // 새로 추가: 내 시간표 과목 전체 정보(CourseDto) 리턴
    @Transactional(readOnly = true)
    public List<CourseDto> myCourses(Long userId) {
        List<TimetableItem> items =
                itemRepo.findByUserIdAndSemesterCode(userId, SemesterConst.SEMESTER_CODE);

        if (items.isEmpty()) return List.of();

        List<Long> courseIds = items.stream()
                .map(TimetableItem::getCourseId)
                .toList();

        List<Course> courses = courseRepo.findAllById(courseIds);
        if (courses.isEmpty()) return List.of();

        Map<Long, List<CourseTime>> timesByCourseId = timeRepo.findByCourseIdIn(courseIds).stream()
                .collect(Collectors.groupingBy(ct -> ct.getCourse().getId()));

        // 리뷰/평점은 timetable 화면에서 필요 없으므로 null 로 둠
        return courses.stream()
                .map(c -> CourseDto.from(
                        c,
                        timesByCourseId.getOrDefault(c.getId(), List.of()),
                        null,
                        null
                ))
                .toList();
    }

    // 새로 추가: 내 시간표 총 학점
    @Transactional(readOnly = true)
    public int myTotalCredits(Long userId) {
        List<TimetableItem> items =
                itemRepo.findByUserIdAndSemesterCode(userId, SemesterConst.SEMESTER_CODE);

        if (items.isEmpty()) {
            return 0;
        }

        List<Long> courseIds = items.stream()
                .map(TimetableItem::getCourseId)
                .toList();

        List<Course> courses = courseRepo.findAllById(courseIds);
        if (courses.isEmpty()) {
            return 0;
        }

        return courses.stream()
                .map(Course::getCredit)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }

    // 1) 시도: 충돌 없으면 즉시 추가, 있으면 conflict=true 만 반환
    @Transactional
    public TryAddResponse tryAdd(Long userId, Long courseId) {
        ensureNotDuplicated(userId, courseId);
        Course newCourse = getCourse(courseId);
        List<CourseTime> newSlots = timeRepo.findByCourseId(courseId);

        List<ConflictHolder> conflicts = findConflicts(userId, newSlots);
        if (!conflicts.isEmpty()) {
            return TryAddResponse.builder().conflict(true).build();
        }

        // 충돌 없음 → 즉시 추가
        AddOpResult r = addItemAndEvents(userId, newCourse, newSlots);
        return TryAddResponse.builder()
                .conflict(false)
                .itemId(r.itemId())
                .createdEventCount(r.createdEvents())
                .build();
    }

    // 2) 해결: KEEP(추가 안 함) / REPLACE(기존 겹치는 항목 삭제 + 추가)
    @Transactional
    public ResolveResult resolve(Long userId, ResolveRequest req) {
        Long courseId = req.getCourseId();
        ensureNotDuplicated(userId, courseId); // 이미 들어가 있으면 409
        Course newCourse = getCourse(courseId);
        List<CourseTime> newSlots = timeRepo.findByCourseId(courseId);

        List<ConflictHolder> conflicts = findConflicts(userId, newSlots);

        if (conflicts.isEmpty()) {
            AddOpResult r = addItemAndEvents(userId, newCourse, newSlots);
            return ResolveResult.builder()
                    .applied(true)
                    .itemId(r.itemId())
                    .createdEventCount(r.createdEvents())
                    .removedItemIds(List.of())
                    .deletedEventCount(0)
                    .build();
        }

        if (req.getResolution() == ConflictResolution.KEEP) {
            return ResolveResult.builder()
                    .applied(false)
                    .removedItemIds(List.of())
                    .deletedEventCount(0)
                    .build();
        }

        // REPLACE: 충돌 항목 삭제 후 추가
        List<Long> removedItemIds = conflicts.stream()
                .map(c -> c.item.getId()).distinct().toList();
        int deletedEvents = 0;
        for (Long id : removedItemIds) {
            deletedEvents += deleteItemAndCalendar(userId, id);
        }

        AddOpResult r = addItemAndEvents(userId, newCourse, newSlots);
        return ResolveResult.builder()
                .applied(true)
                .itemId(r.itemId())
                .createdEventCount(r.createdEvents())
                .removedItemIds(removedItemIds)
                .deletedEventCount(deletedEvents)
                .build();
    }

    @Transactional
    public void remove(Long userId, Long itemId) {
        deleteItemAndCalendar(userId, itemId);
    }

    // ===== 내부 유틸 =====

    private void ensureNotDuplicated(Long userId, Long courseId) {
        if (itemRepo.findByUserIdAndSemesterCodeAndCourseId(userId, SemesterConst.SEMESTER_CODE, courseId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 내 시간표에 존재합니다");
        }
    }

    private Course getCourse(Long courseId) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "강의가 존재하지 않습니다"));
        if (!SemesterConst.SEMESTER_CODE.equals(c.getSemesterCode())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "학기 불일치");
        }
        return c;
    }

    private List<ConflictHolder> findConflicts(Long userId, List<CourseTime> newSlots) {
        List<TimetableItem> existingItems =
                itemRepo.findByUserIdAndSemesterCode(userId, SemesterConst.SEMESTER_CODE);

        Map<Long, List<CourseTime>> existingTimesByCourse = existingItems.isEmpty()
                ? Map.of()
                : timeRepo.findByCourseIdIn(existingItems.stream().map(TimetableItem::getCourseId).toList())
                .stream().collect(Collectors.groupingBy(ct -> ct.getCourse().getId()));

        List<ConflictHolder> conflicts = new ArrayList<>();
        for (TimetableItem item : existingItems) {
            List<CourseTime> times = existingTimesByCourse.getOrDefault(item.getCourseId(), List.of());
            for (CourseTime ex : times) {
                for (CourseTime nv : newSlots) {
                    if (ex.getDayOfWeek() == nv.getDayOfWeek() &&
                            overlaps(ex.getStartTime(), ex.getEndTime(), nv.getStartTime(), nv.getEndTime())) {
                        conflicts.add(new ConflictHolder(item, ex, nv));
                    }
                }
            }
        }
        return conflicts;
    }

    private AddOpResult addItemAndEvents(Long userId, Course newCourse, List<CourseTime> newSlots) {
        TimetableItem saved = itemRepo.save(TimetableItem.builder()
                .userId(userId)
                .semesterCode(SemesterConst.SEMESTER_CODE)
                .courseId(newCourse.getId())
                .build());

        int createdEvents = 0;
        for (CourseTime slot : newSlots) {
            LocalDate first = firstOccurrence(slot.getDayOfWeek(), SemesterConst.SEMESTER_START);
            for (LocalDate d = first; !d.isAfter(SemesterConst.SEMESTER_END); d = d.plusWeeks(1)) {
                LocalDateTime start = LocalDateTime.of(d, slot.getStartTime());
                LocalDateTime end   = LocalDateTime.of(d, slot.getEndTime());
                Long evId = calendarService.createLectureEvent(
                        titleOf(newCourse), locationOf(slot), start, end, userId);
                mapRepo.save(TimetableCalendarMap.builder()
                        .timetableItemId(saved.getId())
                        .calendarEventId(evId)
                        .build());
                createdEvents++;
            }
        }
        return new AddOpResult(saved.getId(), createdEvents);
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private int deleteItemAndCalendar(Long userId, Long itemId) {
        TimetableItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "시간표 항목 없음"));
        if (!item.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 항목만 삭제할 수 있습니다");
        }
        var maps = mapRepo.findByTimetableItemId(itemId);
        var evIds = maps.stream().map(TimetableCalendarMap::getCalendarEventId).toList();
        if (!evIds.isEmpty()) {
            calendarService.deleteEventsByIdsForOwner(evIds, userId);
        }
        mapRepo.deleteByTimetableItemId(itemId);
        itemRepo.delete(item);
        return evIds.size();
    }

    private LocalDate firstOccurrence(DayOfWeek dow, LocalDate start) {
        int diff = (dow.getValue() - start.getDayOfWeek().getValue() + 7) % 7;
        return start.plusDays(diff);
    }

    private String titleOf(Course c) {
        String base = c.getName();
        if (c.getProfessor() != null && !c.getProfessor().isBlank()) base += " (" + c.getProfessor() + ")";
        if (c.getSection() != null && !c.getSection().isBlank())     base += " - " + c.getSection();
        return base;
    }

    private String locationOf(CourseTime t) {
        String room = t.getRoom();
        return (room == null || room.isBlank()) ? null : room;
    }

    private final Map<Long, String> courseNameCache = new HashMap<>();
    private String courseName(Long courseId) {
        return courseNameCache.computeIfAbsent(courseId, id ->
                courseRepo.findById(id).map(Course::getName).orElse("unknown"));
    }

    private class ConflictHolder {
        TimetableItem item;
        CourseTime ex;
        CourseTime nv;
        ConflictHolder(TimetableItem item, CourseTime ex, CourseTime nv) {
            this.item = item; this.ex = ex; this.nv = nv;
        }
        String itemCourseName() { return courseName(item.getCourseId()); }
    }

    private record AddOpResult(Long itemId, int createdEvents) {}
}
