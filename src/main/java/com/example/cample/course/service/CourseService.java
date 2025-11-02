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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepo;
    private final CourseTimeRepository timeRepo;
    private final CourseCategoryRepository categoryRepo;
    private final CourseReviewRepository reviewRepo;

    @Transactional(readOnly = true)
    public List<CategoryNodeDto> categories() {
        var roots = categoryRepo.findByParentIsNull();
        return roots.stream().map(this::toNode).toList();
    }

    private CategoryNodeDto toNode(CourseCategory c) {
        var children = categoryRepo.findByParentId(c.getId()).stream()
                .map(this::toNode).toList();
        return CategoryNodeDto.from(c, children);
    }

    /* year 추가 */
    @Transactional(readOnly = true)
    public Page<CourseDto> search(String q, String professor, Long categoryId, Integer year, Pageable pageable) {
        Page<Course> page = courseRepo.search(
                SemesterConst.SEMESTER_CODE,
                nullOrTrim(q),
                nullOrTrim(professor),
                categoryId,
                year,                        // ← 추가
                pageable
        );

        var ids = page.getContent().stream().map(Course::getId).toList();
        var times = timeRepo.findByCourseIdIn(ids).stream()
                .collect(Collectors.groupingBy(t -> t.getCourse().getId()));
        Map<Long, Double> avgMap = new HashMap<>();
        Map<Long, Long> cntMap = new HashMap<>();
        for (Long id : ids) {
            Object[] row = reviewRepo.countAndAvg(id);
            Long count = ((Number) row[0]).longValue();
            Double avg  = ((Number) row[1]).doubleValue();
            avgMap.put(id, avg);
            cntMap.put(id, count);
        }

        return page.map(c ->
                CourseDto.from(
                        c,
                        times.getOrDefault(c.getId(), List.of()),
                        avgMap.getOrDefault(c.getId(), 0.0),
                        cntMap.getOrDefault(c.getId(), 0L)
                )
        );
    }

    @Transactional(readOnly = true)
    public CourseDto getOne(Long courseId) {
        Course c = courseRepo.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "강의가 존재하지 않습니다"));
        if (!SemesterConst.SEMESTER_CODE.equals(c.getSemesterCode())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "학기 불일치");
        }
        var times = timeRepo.findByCourseId(courseId);
        Object[] row = reviewRepo.countAndAvg(courseId);
        Long count = ((Number) row[0]).longValue();
        Double avg  = ((Number) row[1]).doubleValue();
        return CourseDto.from(c, times, avg, count);
    }

    private String nullOrTrim(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
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
        CourseReview r = existing.orElseGet(() -> CourseReview.builder()
                .course(c).userId(userId).build());
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
        Object[] row = reviewRepo.countAndAvg(courseId);
        Long count = ((Number) row[0]).longValue();
        Double avg  = ((Number) row[1]).doubleValue();
        return Map.of("avg", avg, "count", count);
    }
}
