// src/main/java/com/example/cample/course/service/CourseService.java
package com.example.cample.course.service;

import com.example.cample.common.constant.SemesterConst;
import com.example.cample.common.exception.ApiException;
import com.example.cample.course.domain.*;
import com.example.cample.course.dto.*;
import com.example.cample.course.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepo;
    private final CourseTimeRepository timeRepo;
    private final CourseReviewRepository reviewRepo;

    // 통합 검색
    @Transactional(readOnly = true)
    public List<CourseDto> search(Long categoryId, String startTime, String endTime,
                                  Integer credit, String year, String sort) {
        var list = repoSearch(categoryId, startTime, endTime, credit, year, null, null, null, null);
        var dtos = toDtos(list);
        applySort(dtos, sort);
        return dtos;
    }

    // 단일 검색 4종 (체인 필터/정렬 포함)
    @Transactional(readOnly = true)
    public List<CourseDto> searchByName(String q, Long categoryId, String startTime, String endTime,
                                        Integer credit, String year, String sort) {
        var list = repoSearch(categoryId, startTime, endTime, credit, year, q, null, null, null);
        var dtos = toDtos(list);
        applySort(dtos, sort);
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<CourseDto> searchByProfessor(String professor, Long categoryId, String startTime, String endTime,
                                             Integer credit, String year, String sort) {
        var list = repoSearch(categoryId, startTime, endTime, credit, year, null, professor, null, null);
        var dtos = toDtos(list);
        applySort(dtos, sort);
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<CourseDto> searchByCourseCode(String code, Long categoryId, String startTime, String endTime,
                                              Integer credit, String year, String sort) {
        var list = repoSearch(categoryId, startTime, endTime, credit, year, null, null, code, null);
        var dtos = toDtos(list);
        applySort(dtos, sort);
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<CourseDto> searchByRoom(String room, Long categoryId, String startTime, String endTime,
                                        Integer credit, String year, String sort) {
        var list = repoSearch(categoryId, startTime, endTime, credit, year, null, null, null, room);
        var dtos = toDtos(list);
        applySort(dtos, sort);
        return dtos;
    }

    // 공통 조회 (모든 조건을 한 번에)
    private List<Course> repoSearch(Long categoryId, String startTime, String endTime,
                                    Integer credit, String year, String name, String prof, String code, String room) {
        LocalTime start = parseTimeOrNull(startTime);
        LocalTime end   = parseTimeOrNull(endTime);
        return courseRepo.searchAll(
                SemesterConst.SEMESTER_CODE,
                categoryId,
                credit,
                nullOrTrim(year),
                start, end,
                nullOrTrim(name),
                nullOrTrim(prof),
                nullOrTrim(code),
                nullOrTrim(room)
        );
    }

    private void applySort(List<CourseDto> dtos, String sortRaw) {
        String sort = nullOrTrim(sortRaw);
        if (sort == null || "default".equals(sort)) return;
        switch (sort) {
            case "code" -> dtos.sort(Comparator.comparing(CourseDto::getCourseCode,
                    Comparator.nullsLast(String::compareTo)));
            case "name" -> dtos.sort(this::compareByCustomName);
            case "ratingDesc" -> dtos.sort(Comparator.comparing(CourseDto::getRatingAvg,
                    Comparator.nullsFirst(Double::compareTo)).reversed());
            case "ratingAsc" -> dtos.sort(Comparator.comparing(CourseDto::getRatingAvg,
                    Comparator.nullsFirst(Double::compareTo)));
            default -> { /* 무시 */ }
        }
    }

    @Transactional(readOnly = true)
    public CourseDto getOne(Long courseId) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "강의가 존재하지 않습니다"));
        if (!SemesterConst.SEMESTER_CODE.equals(c.getSemesterCode())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "학기 불일치");
        }
        var times = timeRepo.findByCourseId(courseId);
        var s = statOf(courseId);
        return CourseDto.from(c, times, s.avg(), s.count());
    }

    private String nullOrTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private LocalTime parseTimeOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            if (t.contains(":")) return LocalTime.parse(t); // "13:30"
            if (t.length() == 4)
                return LocalTime.of(Integer.parseInt(t.substring(0, 2)), Integer.parseInt(t.substring(2, 4))); // "1330"
        } catch (Exception ignored) {}
        return null;
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> reviews(Long courseId, Pageable pageable) {
        return reviewRepo.findByCourseIdVisible(courseId, pageable).map(ReviewResponse::from);
    }

    @Transactional
    public ReviewResponse upsertMyReview(Long courseId, Long userId, ReviewRequest req) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "강의가 존재하지 않습니다"));
        if (!SemesterConst.SEMESTER_CODE.equals(c.getSemesterCode())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "학기 불일치");
        }
        var existing = reviewRepo.findByCourseIdAndUserId(courseId, userId);
        CourseReview r = existing.orElseGet(() -> CourseReview.builder().course(c).userId(userId).build());
        r.setRating(req.getRating());
        r.setContent(req.getContent());
        r.setDeleted(false);
        return ReviewResponse.from(reviewRepo.save(r));
    }

    @Transactional
    public void deleteMyReview(Long courseId, Long userId) {
        CourseReview r = reviewRepo.findByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "내 리뷰가 없습니다"));
        r.setDeleted(true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> ratingSummary(Long courseId) {
        var s = statOf(courseId);
        return Map.of("avg", s.avg(), "count", s.count());
    }

    private Stat statOf(Long courseId) {
        CourseReviewRepository.CountAvg s = reviewRepo.countAndAvg(courseId);
        long count = (s != null && s.getCnt() != null) ? s.getCnt() : 0L;
        double avg = (s != null && s.getAvg() != null) ? s.getAvg() : 0.0;
        return new Stat(avg, count);
    }
    private record Stat(double avg, long count) {}

    private List<CourseDto> toDtos(List<Course> courses) {
        if (courses.isEmpty()) return List.of();
        var ids = courses.stream().map(Course::getId).toList();
        var times = timeRepo.findByCourseIdIn(ids).stream()
                .collect(Collectors.groupingBy(t -> t.getCourse().getId()));

        Map<Long, Double> avgMap = new HashMap<>();
        Map<Long, Long> cntMap = new HashMap<>();
        for (Long id : ids) {
            var s = statOf(id);
            avgMap.put(id, s.avg());
            cntMap.put(id, s.count());
        }

        return courses.stream().map(c ->
                CourseDto.from(
                        c,
                        times.getOrDefault(c.getId(), List.of()),
                        avgMap.getOrDefault(c.getId(), 0.0),
                        cntMap.getOrDefault(c.getId(), 0L)
                )
        ).toList();
    }

    // 과목명 커스텀 정렬: 숫자 → 영어(A~Z) → 한글 초성(ㄱ~ㅎ) → 기타
    private int compareByCustomName(CourseDto a, CourseDto b) {
        return nameKey(a.getName()).compareTo(nameKey(b.getName()));
    }
    private NameKey nameKey(String s) {
        if (s == null || s.isBlank()) return new NameKey(99, "", -1);
        char ch = s.charAt(0);
        if (Character.isDigit(ch)) {
            int num = 0;
            for (int i = 0; i < s.length() && Character.isDigit(s.charAt(i)); i++) num = num * 10 + (s.charAt(i) - '0');
            return new NameKey(0, s, num);
        }
        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) return new NameKey(1, s.toUpperCase(Locale.ROOT), -1);
        int init = initialKoreanIndex(ch);
        if (init >= 0) return new NameKey(2, s, init);
        return new NameKey(3, s, -1);
    }
    private int initialKoreanIndex(char ch) {
        if (ch < 0xAC00 || ch > 0xD7A3) return -1;
        int base = ch - 0xAC00;
        return base / 588;
    }
    private record NameKey(int group, String norm, int aux) implements Comparable<NameKey> {
        @Override public int compareTo(NameKey o) {
            if (group != o.group) return Integer.compare(group, o.group);
            if (group == 0) {
                int c = Integer.compare(aux, o.aux);
                if (c != 0) return c;
            }
            int c = norm.compareTo(o.norm);
            if (c != 0) return c;
            return Integer.compare(aux, o.aux);
        }
    }
}
