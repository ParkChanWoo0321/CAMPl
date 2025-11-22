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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService service;

    // 통합 검색(단일 키워드 선택: q | professor | courseCode | room)
    // 확장: years(다중 CSV), yearsEtcOnly=true면 1,2,3,4 제외 나머지 연도만, credits(다중 CSV: 0,0.5,...,3.5,4+)
    @GetMapping
    public List<CourseDto> search(
            @RequestParam(required = false) Long categoryId,

            // 기존 단일 → 유지(없애지 않음)
            @RequestParam(required = false) Integer credit,
            @RequestParam(required = false) String year,

            // 신규 다중 필터
            @RequestParam(required = false) String years,               // 예: "1,2,3,4" 또는 "1,3"
            @RequestParam(required = false, defaultValue = "false") boolean yearsEtcOnly, // true면 1,2,3,4 제외 나머지
            @RequestParam(required = false) String credits,             // 예: "0,1.5,3,4+"

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
        if (has(name)) modeCount++;
        if (has(professor)) modeCount++;
        if (has(courseCode)) modeCount++;
        if (has(room)) modeCount++;
        if (modeCount > 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "q/professor/courseCode/room 중 하나만 지정하세요");
        }

        // 단일 파라미터와 신규 다중 파라미터 병합
        List<String> yearList = mergeYears(years, year);
        List<String> creditList = mergeCredits(credits, credit);

        if (has(name)) {
            return service.searchByNameV2(
                    name, categoryId, yearList, yearsEtcOnly, creditList,
                    sort, days, startTime, endTime, ranges, windows
            );
        }
        if (has(professor)) {
            return service.searchByProfessorV2(
                    professor, categoryId, yearList, yearsEtcOnly, creditList,
                    sort, days, startTime, endTime, ranges, windows
            );
        }
        if (has(courseCode)) {
            return service.searchByCourseCodeV2(
                    courseCode, categoryId, yearList, yearsEtcOnly, creditList,
                    sort, days, startTime, endTime, ranges, windows
            );
        }
        if (has(room)) {
            return service.searchByRoomV2(
                    room, categoryId, yearList, yearsEtcOnly, creditList,
                    sort, days, startTime, endTime, ranges, windows
            );
        }

        return service.searchV2(
                categoryId, yearList, yearsEtcOnly, creditList,
                sort, days, startTime, endTime, ranges, windows
        );
    }

    // 단건: 상세 + 평점 + 강의평 목록(기본: 최신순)
    @GetMapping("/{courseId}")
    public CourseDto getOne(@PathVariable Long courseId) {
        return service.getOne(courseId);
    }

    // ===== 강의평 조회(정렬별) =====

    // 최신순(createdAt DESC)
    @GetMapping("/reviews/latest/{courseId}")
    public List<ReviewResponse> getReviewsLatest(@PathVariable Long courseId) {
        return service.getReviewsSorted(courseId, "latest");
    }

    // 오래된순(createdAt ASC)
    @GetMapping("/reviews/oldest/{courseId}")
    public List<ReviewResponse> getReviewsOldest(@PathVariable Long courseId) {
        return service.getReviewsSorted(courseId, "oldest");
    }

    // 별점 높은순(rating DESC → createdAt DESC)
    @GetMapping("/reviews/rating-high/{courseId}")
    public List<ReviewResponse> getReviewsRatingHigh(@PathVariable Long courseId) {
        return service.getReviewsSorted(courseId, "ratingDesc");
    }

    // 별점 낮은순(rating ASC → createdAt ASC)
    @GetMapping("/reviews/rating-low/{courseId}")
    public List<ReviewResponse> getReviewsRatingLow(@PathVariable Long courseId) {
        return service.getReviewsSorted(courseId, "ratingAsc");
    }

    // ===== 강의평 작성/수정/삭제 =====

    // 생성 + 업서트(기존 유지)
    @PostMapping("/reviews/{courseId}")
    public ReviewResponse upsertMyReview(@PathVariable Long courseId,
                                         @Valid @RequestBody ReviewRequest req,
                                         @AuthenticationPrincipal CustomUserPrincipal me) {
        return service.upsertMyReview(courseId, me.getId(), req);
    }

    // 수정 전용(내 리뷰 없으면 404)
    @PutMapping("/reviews/{courseId}")
    public ReviewResponse updateMyReview(@PathVariable Long courseId,
                                         @Valid @RequestBody ReviewRequest req,
                                         @AuthenticationPrincipal CustomUserPrincipal me) {
        return service.updateMyReview(courseId, me.getId(), req);
    }

    @DeleteMapping("/reviews/me/{courseId}")
    public Map<String, Object> deleteMyReview(@PathVariable Long courseId,
                                              @AuthenticationPrincipal CustomUserPrincipal me) {
        service.deleteMyReview(courseId, me.getId());
        return Map.of("ok", true);
    }

    // ===== 내부 유틸 =====
    private boolean has(String s) {
        return s != null && !s.isBlank();
    }

    private List<String> mergeYears(String yearsCsv, String singleYear) {
        List<String> list = parseCsv(yearsCsv);
        if (!list.isEmpty()) return list;
        if (has(singleYear)) return List.of(singleYear.trim());
        return List.of(); // 필터 없음
    }

    private List<String> mergeCredits(String creditsCsv, Integer singleCredit) {
        List<String> list = parseCsv(creditsCsv);
        if (!list.isEmpty()) return normalizeCredits(list);
        if (singleCredit != null) return List.of(String.valueOf(singleCredit));
        return List.of(); // 필터 없음
    }

    private List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    // 허용 토큰: "0","0.5","1","1.5","2","2.5","3","3.5","4+"
    private List<String> normalizeCredits(List<String> tokens) {
        return tokens.stream()
                .map(t -> t.equalsIgnoreCase("4plus") ? "4+" : t)
                .collect(Collectors.toList());
    }
}
