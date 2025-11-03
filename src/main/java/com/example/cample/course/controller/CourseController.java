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

    // 통합 검색: categoryId, startTime, endTime, credit, year + sort
    // 예) /api/courses?categoryId=29&startTime=13:30&endTime=16:30&credit=3&year=2&sort=name
    @GetMapping
    public List<CourseDto> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String startTime,   // "13:30" 또는 "1330"
            @RequestParam(required = false) String endTime,     // "16:30" 또는 "1630"
            @RequestParam(required = false) Integer credit,
            @RequestParam(required = false) String year,        // "1"~"4" 또는 "모든학년"
            @RequestParam(required = false, defaultValue = "default")
            String sort                                         // default|code|name|ratingDesc|ratingAsc
    ) {
        return service.search(categoryId, startTime, endTime, credit, year, sort);
    }

    // 단일 검색 4종 (여기도 동일 필터/정렬 추가로 받아서 바로 체인 가능)
    @GetMapping("/search/name")
    public List<CourseDto> searchByName(@RequestParam String q,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String startTime,
                                        @RequestParam(required = false) String endTime,
                                        @RequestParam(required = false) Integer credit,
                                        @RequestParam(required = false) String year,
                                        @RequestParam(defaultValue = "default") String sort) {
        return service.searchByName(q, categoryId, startTime, endTime, credit, year, sort);
    }

    @GetMapping("/search/professor")
    public List<CourseDto> searchByProfessor(@RequestParam String professor,
                                             @RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false) String startTime,
                                             @RequestParam(required = false) String endTime,
                                             @RequestParam(required = false) Integer credit,
                                             @RequestParam(required = false) String year,
                                             @RequestParam(defaultValue = "default") String sort) {
        return service.searchByProfessor(professor, categoryId, startTime, endTime, credit, year, sort);
    }

    @GetMapping("/search/code")
    public List<CourseDto> searchByCourseCode(@RequestParam String courseCode,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) String startTime,
                                              @RequestParam(required = false) String endTime,
                                              @RequestParam(required = false) Integer credit,
                                              @RequestParam(required = false) String year,
                                              @RequestParam(defaultValue = "default") String sort) {
        return service.searchByCourseCode(courseCode, categoryId, startTime, endTime, credit, year, sort);
    }

    @GetMapping("/search/room")
    public List<CourseDto> searchByRoom(@RequestParam String room,
                                        @RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String startTime,
                                        @RequestParam(required = false) String endTime,
                                        @RequestParam(required = false) Integer credit,
                                        @RequestParam(required = false) String year,
                                        @RequestParam(defaultValue = "default") String sort) {
        return service.searchByRoom(room, categoryId, startTime, endTime, credit, year, sort);
    }

    // 단건/리뷰 API 유지
    @GetMapping("/{courseId}")
    public CourseDto getOne(@PathVariable Long courseId) {
        return service.getOne(courseId);
    }

    @GetMapping("/reviews/{courseId}")
    public org.springframework.data.domain.Page<ReviewResponse> reviews(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.reviews(courseId, org.springframework.data.domain.PageRequest.of(page, size));
    }

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

    @GetMapping("/rating/{courseId}")
    public Map<String, Object> rating(@PathVariable Long courseId) {
        return service.ratingSummary(courseId);
    }
}
