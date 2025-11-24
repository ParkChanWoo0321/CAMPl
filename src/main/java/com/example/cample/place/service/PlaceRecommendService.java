// src/main/java/com/example/cample/place/service/PlaceRecommendService.java
package com.example.cample.place.service;

import com.example.cample.place.dto.PlaceRecommendRequest;
import com.example.cample.place.dto.PlaceSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceRecommendService {

    private final PlaceService placeService;
    private final PlaceGptClient gptClient;

    @Transactional(readOnly = true)
    public List<PlaceSummaryDto> recommend(PlaceRecommendRequest req) {
        // 항상 3개 고정
        final int limit = 3;

        // 1) 기본 후보: 현재는 전체 장소
        List<PlaceSummaryDto> candidates = placeService.findAllSummaries();
        if (candidates.isEmpty()) {
            return List.of();
        }

        // 2) GPT에게 후보 목록 + 조건을 넘겨서 id 리스트 받아오기
        List<Long> pickedIds = gptClient.choosePlaceIds(
                req.getScheduleType(),
                req.getWithWhom(),
                candidates,
                limit
        );

        // 3) GPT 응답이 유효하면 그 순서대로 반환
        if (pickedIds != null && !pickedIds.isEmpty()) {
            return placeService.summariesByIdsInOrder(pickedIds);
        }

        // 4) GPT 실패 시: 랜덤 셔플 + 상위 3개 fallback
        Collections.shuffle(candidates);
        if (candidates.size() <= limit) {
            return candidates;
        }
        return candidates.subList(0, limit);
    }
}
