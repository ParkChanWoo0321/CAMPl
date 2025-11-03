// src/main/java/com/example/cample/course/controller/CourseController.java
package com.example.cample.course.controller;

import com.example.cample.course.dto.*;
import com.example.cample.course.service.CourseService;
import com.example.cample.security.model.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService service;

    // 통합 검색
    @GetMapping
    public List<CourseDto> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer credit,
            @RequestParam(required = false) String year,
            @RequestParam(required = false, defaultValue = "default") String sort,
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String ranges,
            @RequestParam(required = false) String windows
    ) {
        return service.search(categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    // 단일 검색 4종
    @GetMapping("/search/name")
    public List<CourseDto> searchByName(
            @RequestParam String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer credit,
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String ranges,
            @RequestParam(required = false) String windows
    ) {
        return service.searchByName(q, categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    @GetMapping("/search/professor")
    public List<CourseDto> searchByProfessor(
            @RequestParam String professor,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer credit,
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String ranges,
            @RequestParam(required = false) String windows
    ) {
        return service.searchByProfessor(professor, categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    @GetMapping("/search/code")
    public List<CourseDto> searchByCourseCode(
            @RequestParam String courseCode,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer credit,
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String ranges,
            @RequestParam(required = false) String windows
    ) {
        return service.searchByCourseCode(courseCode, categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    @GetMapping("/search/room")
    public List<CourseDto> searchByRoom(
            @RequestParam String room,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer credit,
            @RequestParam(required = false) String year,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(required = false) String days,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String ranges,
            @RequestParam(required = false) String windows
    ) {
        return service.searchByRoom(room, categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
    }

    // 단건: 상세 + 평점 + 강의평 목록
    @GetMapping("/{courseId}")
    public CourseDto getOne(@PathVariable Long courseId) {
        return service.getOne(courseId);
    }

    // 리뷰 작성/수정, 삭제(유지)
    @PostMapping("/reviews/{courseId}")
    public ReviewResponse upsertMyReview(@PathVariable Long courseId,
                                         @Valid @RequestBody ReviewRequest req,
                                         @AuthenticationPrincipal CustomUserPrincipal me) {
        return service.upsertMyReview(courseId, me.getId(), req);
    }

    @DeleteMapping("/reviews/me/{courseId}")
    public Map<String, Object> deleteMyReview(@PathVariable Long courseId,
                                              @AuthenticationPrincipal CustomUserPrincipal me) {
        service.deleteMyReview(courseId, me.getId());
        return Map.of("ok", true);
    }
}
