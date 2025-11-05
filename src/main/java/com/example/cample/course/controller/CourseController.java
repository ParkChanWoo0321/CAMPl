// src/main/java/com/example/cample/course/controller/CourseController.java
package com.example.cample.course.controller;

import com.example.cample.common.exception.ApiException;
import com.example.cample.course.dto.*;
import com.example.cample.course.service.CourseService;
import com.example.cample.security.model.CustomUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService service;

    // 통합 검색(단일 키워드 선택: q | professor | courseCode | room)
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
            @RequestParam(required = false) String windows,
            // 단일 선택 키워드(4중 1개만 지정)
            @RequestParam(required = false, name = "q") String name,
            @RequestParam(required = false) String professor,
            @RequestParam(required = false) String courseCode,
            @RequestParam(required = false) String room
    ) {
        int modeCount = 0;
        if (name != null && !name.isBlank()) modeCount++;
        if (professor != null && !professor.isBlank()) modeCount++;
        if (courseCode != null && !courseCode.isBlank()) modeCount++;
        if (room != null && !room.isBlank()) modeCount++;
        if (modeCount > 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "q/professor/courseCode/room 중 하나만 지정하세요");
        }

        if (name != null && !name.isBlank()) {
            return service.searchByName(name, categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
        }
        if (professor != null && !professor.isBlank()) {
            return service.searchByProfessor(professor, categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
        }
        if (courseCode != null && !courseCode.isBlank()) {
            return service.searchByCourseCode(courseCode, categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
        }
        if (room != null && !room.isBlank()) {
            return service.searchByRoom(room, categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
        }

        return service.search(categoryId, credit, year, sort, days, startTime, endTime, ranges, windows);
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
