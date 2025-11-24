// src/main/java/com/example/cample/place/controller/PlaceController.java
package com.example.cample.place.controller;

import com.example.cample.place.dto.PlaceDetailDto;
import com.example.cample.place.dto.PlaceRecommendRequest;
import com.example.cample.place.dto.PlaceSummaryDto;
import com.example.cample.place.service.PlaceRecommendService;
import com.example.cample.place.service.PlaceService;
import com.example.cample.security.model.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;
    private final PlaceRecommendService recommendService;

    // 추천 받기 (첫 번째/두 번째 사진의 리스트용)
    @PostMapping("/recommend")
    public List<PlaceSummaryDto> recommend(
            @RequestBody PlaceRecommendRequest req,
            @AuthenticationPrincipal CustomUserPrincipal me
    ) {
        // 지금은 me.getId() 를 안 쓰지만,
        // 나중에 "내 시간표/일정 기반 추천"으로 확장할 때 사용 가능
        return recommendService.recommend(req);
    }

    // 단건 상세 (카드 클릭 시: 사진 + 메뉴판)
    @GetMapping("/{placeId}")
    public PlaceDetailDto getOne(
            @PathVariable Long placeId,
            @AuthenticationPrincipal CustomUserPrincipal me
    ) {
        return placeService.getDetail(placeId);
    }
}
