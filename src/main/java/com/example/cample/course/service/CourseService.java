// src/main/java/com/example/cample/course/service/CourseService.java
package com.example.cample.course.service;

import com.example.cample.common.constant.SemesterConst;
import com.example.cample.common.exception.ApiException;
import com.example.cample.course.domain.*;
import com.example.cample.course.dto.*;
import com.example.cample.course.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepo;
    private final CourseTimeRepository timeRepo;
    private final CourseReviewRepository reviewRepo;

    // ===== 통합 검색 =====
    @Transactional(readOnly = true)
    public List<CourseDto> search(Long categoryId, Integer credit, String year, String sort,
                                  String days, String startTime, String endTime, String ranges, String windows) {
        return searchInternal(null, null, null, null,
                categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    // ===== 단일 검색 4종 =====
    @Transactional(readOnly = true)
    public List<CourseDto> searchByName(String q, Long categoryId, Integer credit, String year, String sort,
                                        String days, String startTime, String endTime, String ranges, String windows) {
        return searchInternal(q, null, null, null,
                categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> searchByProfessor(String professor, Long categoryId, Integer credit, String year, String sort,
                                             String days, String startTime, String endTime, String ranges, String windows) {
        return searchInternal(null, professor, null, null,
                categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> searchByCourseCode(String code, Long categoryId, Integer credit, String year, String sort,
                                              String days, String startTime, String endTime, String ranges, String windows) {
        return searchInternal(null, null, code, null,
                categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    @Transactional(readOnly = true)
    public List<CourseDto> searchByRoom(String room, Long categoryId, Integer credit, String year, String sort,
                                        String days, String startTime, String endTime, String ranges, String windows) {
        return searchInternal(null, null, null, room,
                categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    // ===== 내부 공통 =====
    private List<CourseDto> searchInternal(String name, String prof, String code, String room,
                                           Long categoryId, Integer credit, String year, String sort,
                                           String days, String startTime, String endTime, String ranges, String windows) {

        // 1) 파싱
        Set<DayOfWeek> daySet = parseDays(days);
        List<Range> rangeList = parseRanges(ranges);
        Range single = toRange(startTime, endTime);
        if (single != null) rangeList.add(single);
        Map<DayOfWeek, List<Range>> windowMap = parseWindows(windows);

        // 2) 조합 쿼리 (합집합, 중복 제거)
        LinkedHashSet<Course> acc = new LinkedHashSet<>();

        if (!windowMap.isEmpty()) {
            for (var e : windowMap.entrySet()) {
                DayOfWeek d = e.getKey();
                for (Range r : e.getValue()) {
                    acc.addAll(courseRepo.searchOneWindow(
                            SemesterConst.SEMESTER_CODE,
                            categoryId, credit, nullOrTrim(year),
                            nullOrTrim(name), nullOrTrim(prof), nullOrTrim(code), nullOrTrim(room),
                            d, r.start(), r.end()
                    ));
                }
            }
        } else if (!daySet.isEmpty() && !rangeList.isEmpty()) {
            for (DayOfWeek d : daySet) {
                for (Range r : rangeList) {
                    acc.addAll(courseRepo.searchOneWindow(
                            SemesterConst.SEMESTER_CODE,
                            categoryId, credit, nullOrTrim(year),
                            nullOrTrim(name), nullOrTrim(prof), nullOrTrim(code), nullOrTrim(room),
                            d, r.start(), r.end()
                    ));
                }
            }
        } else if (!daySet.isEmpty()) {
            for (DayOfWeek d : daySet) {
                acc.addAll(courseRepo.searchOneWindow(
                        SemesterConst.SEMESTER_CODE,
                        categoryId, credit, nullOrTrim(year),
                        nullOrTrim(name), nullOrTrim(prof), nullOrTrim(code), nullOrTrim(room),
                        d, null, null
                ));
            }
        } else if (!rangeList.isEmpty()) {
            for (Range r : rangeList) {
                acc.addAll(courseRepo.searchOneWindow(
                        SemesterConst.SEMESTER_CODE,
                        categoryId, credit, nullOrTrim(year),
                        nullOrTrim(name), nullOrTrim(prof), nullOrTrim(code), nullOrTrim(room),
                        null, r.start(), r.end()
                ));
            }
        } else {
            acc.addAll(courseRepo.searchOneWindow(
                    SemesterConst.SEMESTER_CODE,
                    categoryId, credit, nullOrTrim(year),
                    nullOrTrim(name), nullOrTrim(prof), nullOrTrim(code), nullOrTrim(room),
                    null, null, null
            ));
        }

        // 3) 매핑 + 정렬
        var dtos = toDtos(new ArrayList<>(acc));
        applySort(dtos, sort);
        return dtos;
    }

    // 정렬
    private void applySort(List<CourseDto> dtos, String sortRaw) {
        String sort = nullOrTrim(sortRaw);
        if (sort == null || "default".equals(sort)) return;
        switch (sort) {
            case "code"      -> dtos.sort(Comparator.comparing(CourseDto::getCourseCode, Comparator.nullsLast(String::compareTo)));
            case "name"      -> dtos.sort(this::compareByCustomName);
            case "ratingAsc" -> dtos.sort(Comparator.comparing(CourseDto::getRatingAvg, Comparator.nullsFirst(Double::compareTo)));
            case "ratingDesc"-> dtos.sort(Comparator.comparing(CourseDto::getRatingAvg, Comparator.nullsFirst(Double::compareTo)).reversed());
            default -> {}
        }
    }

    // 단건: 상세 + 평점 + 강의평 목록
    @Transactional(readOnly = true)
    public CourseDto getOne(Long courseId) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "강의가 존재하지 않습니다"));
        if (!SemesterConst.SEMESTER_CODE.equals(c.getSemesterCode())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "학기 불일치");
        }
        var times = timeRepo.findByCourseId(courseId);
        var s = statOf(courseId);
        var reviews = reviewRepo.findByCourseIdAndDeletedFalseOrderByCreatedAtDesc(courseId)
                .stream().map(ReviewResponse::from).toList();

        return CourseDto.fromDetailed(c, times, s.avg(), s.count(), reviews);
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

    private Stat statOf(Long courseId) {
        CourseReviewRepository.CountAvg s = reviewRepo.countAndAvg(courseId);
        long count = (s != null && s.getCnt() != null) ? s.getCnt() : 0L;
        double avg = (s != null && s.getAvg() != null) ? s.getAvg() : 0.0;
        return new Stat(avg, count);
    }
    private record Stat(double avg, long count) {}

    // ===== 매핑/정렬 유틸 =====
    private List<CourseDto> toDtos(List<Course> courses) {
        if (courses.isEmpty()) return new ArrayList<>();
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
        ).collect(Collectors.toCollection(ArrayList::new));
    }

    // 과목명 커스텀 정렬: 숫자 → 영어 → 한글초성 → 기타
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
            if (group == 0) { int c = Integer.compare(aux, o.aux); if (c != 0) return c; }
            int c = norm.compareTo(o.norm); if (c != 0) return c;
            return Integer.compare(aux, o.aux);
        }
    }

    // ===== 파싱 유틸 =====
    private String nullOrTrim(String s) { if (s == null) return null; s = s.trim(); return s.isEmpty() ? null : s; }

    private Set<DayOfWeek> parseDays(String s) {
        if (s == null || s.isBlank()) return Collections.emptySet();
        Set<DayOfWeek> out = new LinkedHashSet<>();
        for (String tok : s.split(",")) {
            String t = tok.trim().toUpperCase(Locale.ROOT);
            switch (t) {
                case "MON" -> out.add(DayOfWeek.MONDAY);
                case "TUE" -> out.add(DayOfWeek.TUESDAY);
                case "WED" -> out.add(DayOfWeek.WEDNESDAY);
                case "THU" -> out.add(DayOfWeek.THURSDAY);
                case "FRI" -> out.add(DayOfWeek.FRIDAY);
                case "SAT" -> out.add(DayOfWeek.SATURDAY);
                case "SUN" -> out.add(DayOfWeek.SUNDAY);
                default -> {}
            }
        }
        return out;
    }

    private List<Range> parseRanges(String s) {
        if (s == null || s.isBlank()) return new ArrayList<>();
        List<Range> out = new ArrayList<>();
        for (String tok : s.split(",")) {
            Range r = parseRange(tok.trim());
            if (r != null) out.add(r);
        }
        return out;
    }

    private Map<DayOfWeek, List<Range>> parseWindows(String s) {
        Map<DayOfWeek, List<Range>> map = new LinkedHashMap<>();
        if (s == null || s.isBlank()) return map;
        for (String dayPart : s.split(";")) {
            String part = dayPart.trim();
            if (part.isEmpty()) continue;
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            DayOfWeek day = switch (kv[0].trim().toUpperCase(Locale.ROOT)) {
                case "MON" -> DayOfWeek.MONDAY;
                case "TUE" -> DayOfWeek.TUESDAY;
                case "WED" -> DayOfWeek.WEDNESDAY;
                case "THU" -> DayOfWeek.THURSDAY;
                case "FRI" -> DayOfWeek.FRIDAY;
                case "SAT" -> DayOfWeek.SATURDAY;
                case "SUN" -> DayOfWeek.SUNDAY;
                default -> null;
            };
            if (day == null) continue;
            List<Range> list = new ArrayList<>();
            for (String rTok : kv[1].split(",")) {
                Range r = parseRange(rTok.trim());
                if (r != null) list.add(r);
            }
            if (!list.isEmpty()) map.put(day, list);
        }
        return map;
    }

    private Range parseRange(String s) {
        if (s == null || s.isBlank()) return null;
        String[] ab = s.split("-");
        if (ab.length != 2) return null;
        LocalTime a = parseTime(ab[0].trim());
        LocalTime b = parseTime(ab[1].trim());
        if (a == null || b == null) return null;
        return new Range(a, b);
    }

    private Range toRange(String start, String end) {
        if (start == null && end == null) return null;
        LocalTime s = parseTime(start);
        LocalTime e = parseTime(end);
        if (s == null && e == null) return null;
        return new Range(s, e);
    }

    private LocalTime parseTime(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            if (t.contains(":")) return LocalTime.parse(t);
            if (t.length() == 4) return LocalTime.of(
                    Integer.parseInt(t.substring(0, 2)),
                    Integer.parseInt(t.substring(2, 4))
            );
        } catch (Exception ignored) {}
        return null;
    }

    private record Range(LocalTime start, LocalTime end) {}
}
