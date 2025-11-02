package com.example.cample.timetable.controller;

import com.example.cample.common.constant.SemesterConst;
import com.example.cample.security.model.CustomUserPrincipal;
import com.example.cample.timetable.dto.AddCourseRequest;
import com.example.cample.timetable.dto.AddResult;
import com.example.cample.timetable.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetable")
public class TimetableController {

    private final TimetableService service;

    @GetMapping
    public Map<String, Object> myTimetable(@org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal me) {
        List<Long> courseIds = service.myCourseIds(me.getId());
        return Map.of(
                "semester", SemesterConst.SEMESTER_CODE,
                "courseIds", courseIds
        );
    }

    @PostMapping("/items")
    public AddResult add(@Valid @RequestBody AddCourseRequest req,
                         @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal me) {
        return service.add(me.getId(), req);
    }

    @DeleteMapping("/items/{itemId}")
    public Map<String, Object> remove(@PathVariable Long itemId,
                                      @org.springframework.security.core.annotation.AuthenticationPrincipal CustomUserPrincipal me) {
        service.remove(me.getId(), itemId);
        return Map.of("ok", true);
    }
}
