package com.example.cample.timetable.service;

import com.example.cample.calendar.service.CalendarService;
import com.example.cample.common.constant.SemesterConst;
import com.example.cample.common.exception.ApiException;
import com.example.cample.course.domain.Course;
import com.example.cample.course.domain.CourseTime;
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

    @Transactional(readOnly = true)
    public List<Long> myCourseIds(Long userId) {
        return itemRepo.findByUserIdAndSemesterCode(userId, SemesterConst.SEMESTER_CODE)
                .stream().map(TimetableItem::getCourseId).toList();
    }

    @Transactional
    public AddResult add(Long userId, AddCourseRequest req) {
        Long courseId = req.getCourseId();

        if (itemRepo.findByUserIdAndSemesterCodeAndCourseId(userId, SemesterConst.SEMESTER_CODE, courseId).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "이미 내 시간표에 존재합니다");
        }

        Course newCourse = courseRepo.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "강의가 존재하지 않습니다"));
        if (!SemesterConst.SEMESTER_CODE.equals(newCourse.getSemesterCode())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "학기 불일치");
        }
        List<CourseTime> newSlots = timeRepo.findByCourseId(courseId);

        List<TimetableItem> existingItems =
                itemRepo.findByUserIdAndSemesterCode(userId, SemesterConst.SEMESTER_CODE);

        Map<Long, List<CourseTime>> existingTimesByCourse = timeRepo
                .findByCourseIdIn(existingItems.stream().map(TimetableItem::getCourseId).toList())
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

        List<Long> removedItemIds = new ArrayList<>();
        int deletedEvents = 0;

        if (!conflicts.isEmpty()) {
            if (req.getConflictResolution() == ConflictResolution.KEEP) {
                throwConflict(conflicts, newCourse.getName());
            } else {
                for (Long itemId : conflicts.stream().map(c -> c.item.getId()).distinct().toList()) {
                    deletedEvents += deleteItemAndCalendar(userId, itemId);
                    removedItemIds.add(itemId);
                }
            }
        }

        TimetableItem saved = itemRepo.save(TimetableItem.builder()
                .userId(userId)
                .semesterCode(SemesterConst.SEMESTER_CODE)
                .courseId(courseId)
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

        return AddResult.builder()
                .itemId(saved.getId())
                .removedItemIds(removedItemIds)
                .createdEventCount(createdEvents)
                .deletedEventCount(deletedEvents)
                .build();
    }

    @Transactional
    public void remove(Long userId, Long itemId) {
        deleteItemAndCalendar(userId, itemId);
    }

    private boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private void throwConflict(List<ConflictHolder> conflicts, String newName) {
        List<ConflictDto> dtos = conflicts.stream().map(c -> ConflictDto.builder()
                .existingItemId(c.item.getId())
                .existingCourseId(c.item.getCourseId())
                .existingCourseName(c.itemCourseName())
                .day(c.ex.getDayOfWeek().name())
                .existing(c.ex.getStartTime() + "-" + c.ex.getEndTime())
                .requested(c.nv.getStartTime() + "-" + c.nv.getEndTime())
                .build()).toList();
        throw new ApiException(HttpStatus.CONFLICT, "시간이 겹치는 강의가 있습니다 (REPLACE로 재시도)");
    }

    private int deleteItemAndCalendar(Long userId, Long itemId) {
        TimetableItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "시간표 항목 없음"));
        if (!item.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "본인 항목만 삭제할 수 있습니다");
        }
        var maps = mapRepo.findByTimetableItemId(itemId);
        var evIds = maps.stream().map(TimetableCalendarMap::getCalendarEventId).toList();
        calendarService.deleteEventsByIdsForOwner(evIds, userId);
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
        if (c.getProfessor() != null && !c.getProfessor().isBlank()) {
            base += " (" + c.getProfessor() + ")";
        }
        if (c.getSection() != null && !c.getSection().isBlank()) {
            base += " - " + c.getSection();
        }
        return base;
    }

    /** building 제거 버전: room 만 사용 */
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
}
