// src/main/java/com/example/cample/timetable/controller/TimetableController.java
package com.example.cample.timetable.controller;

import com.example.cample.common.constant.SemesterConst;
import com.example.cample.security.model.CustomUserPrincipal;
import com.example.cample.timetable.dto.*;
import com.example.cample.timetable.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetable")
public class TimetableController {

    private final TimetableService service;

    @GetMapping
    public Map<String, Object> myTimetable(
            @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal me) {
        return Map.of(
                "semester", SemesterConst.SEMESTER_CODE,
                "courses", service.myCourses(me.getId())
        );
    }

    // 내 시간표 총 학점 조회
    @GetMapping("/credits")
    public Map<String, Object> myTotalCredits(
            @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal me) {
        int totalCredits = service.myTotalCredits(me.getId());
        return Map.of(
                "semester", SemesterConst.SEMESTER_CODE,
                "totalCredits", totalCredits
        );
    }

    // 1단계: 충돌 검사 + (충돌 없으면 즉시 추가)
    @PostMapping("/items/try-add")
    public TryAddResponse tryAdd(@Valid @RequestBody AddCourseRequest req,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal me) {
        return service.tryAdd(me.getId(), req.getCourseId());
    }

    // 2단계: 프런트 선택(KEEP/REPLACE) 후 확정
    @PostMapping("/items/resolve")
    public ResolveResult resolve(@Valid @RequestBody ResolveRequest req,
                                 @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal me) {
        return service.resolve(me.getId(), req);
    }

    @DeleteMapping("/items/{itemId}")
    public Map<String, Object> remove(@PathVariable Long itemId,
                                      @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal me) {
        service.remove(me.getId(), itemId);
        return Map.of("ok", true);
    }
}
