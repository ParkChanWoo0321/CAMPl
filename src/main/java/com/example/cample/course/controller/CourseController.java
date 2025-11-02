// src/main/java/com/example/cample/course/controller/CourseController.java
package com.example.cample.course.controller;

import com.example.cample.course.dto.*;
import com.example.cample.course.service.CourseService;
import com.example.cample.security.model.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService service;

    @GetMapping("/categories")
    public List<CategoryNodeDto> categories() {
        return service.categories();
    }

    @GetMapping
    public Page<CourseDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String professor,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer year,   // ← 추가
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return service.search(q, professor, categoryId, year, pageable); // ← year 전달
    }

    @GetMapping("/{courseId}")
    public CourseDto getOne(@PathVariable Long courseId) {
        return service.getOne(courseId);
    }

    @GetMapping("/{courseId}/reviews")
    public Page<ReviewResponse> reviews(@PathVariable Long courseId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.reviews(courseId, PageRequest.of(page, size));
    }

    @PostMapping("/{courseId}/reviews")
    public ReviewResponse upsertMyReview(@PathVariable Long courseId,
                                         @Valid @RequestBody ReviewRequest req,
                                         @AuthenticationPrincipal CustomUserPrincipal me) {
        return service.upsertMyReview(courseId, me.getId(), req);
    }

    @DeleteMapping("/{courseId}/reviews/me")
    public Map<String, Object> deleteMyReview(@PathVariable Long courseId,
                                              @AuthenticationPrincipal CustomUserPrincipal me) {
        service.deleteMyReview(courseId, me.getId());
        return Map.of("ok", true);
    }

    @GetMapping("/{courseId}/rating")
    public Map<String, Object> rating(@PathVariable Long courseId) {
        return service.ratingSummary(courseId);
    }
}
