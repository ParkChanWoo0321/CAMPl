// src/main/java/com/example/cample/place/service/PlaceService.java
package com.example.cample.place.service;

import com.example.cample.common.exception.ApiException;
import com.example.cample.place.domain.Place;
import com.example.cample.place.domain.PlaceMenu;
import com.example.cample.place.domain.PlaceType;
import com.example.cample.place.dto.PlaceDetailDto;
import com.example.cample.place.dto.PlaceSummaryDto;
import com.example.cample.place.repo.PlaceMenuRepository;
import com.example.cample.place.repo.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepo;
    private final PlaceMenuRepository menuRepo;

    // 전체 요약 목록
    @Transactional(readOnly = true)
    public List<PlaceSummaryDto> findAllSummaries() {
        return placeRepo.findAll().stream()
                .map(PlaceSummaryDto::from)
                .collect(Collectors.toList());
    }

    // 타입으로 필터 (식당/카페/술집)
    @Transactional(readOnly = true)
    public List<PlaceSummaryDto> findSummariesByType(PlaceType type) {
        return placeRepo.findByType(type).stream()
                .map(PlaceSummaryDto::from)
                .collect(Collectors.toList());
    }

    // 여러 타입 한 번에 (필요하면 사용)
    @Transactional(readOnly = true)
    public List<PlaceSummaryDto> findSummariesByTypes(List<PlaceType> types) {
        if (types == null || types.isEmpty()) {
            return findAllSummaries();
        }
        return placeRepo.findByTypeIn(types).stream()
                .map(PlaceSummaryDto::from)
                .collect(Collectors.toList());
    }

    // 단건 상세 (사진 + 메뉴판 전체)
    @Transactional(readOnly = true)
    public PlaceDetailDto getDetail(Long placeId) {
        Place p = placeRepo.findById(placeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "장소가 존재하지 않습니다"));
        List<PlaceMenu> menus = menuRepo.findByPlaceId(placeId);
        return PlaceDetailDto.from(p, menus);
    }

    // 추천 결과용: 주어진 id 순서를 유지해서 요약 DTO로 변환
    @Transactional(readOnly = true)
    public List<PlaceSummaryDto> summariesByIdsInOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Place> places = placeRepo.findAllById(ids);
        Map<Long, Place> map = places.stream()
                .collect(Collectors.toMap(Place::getId, p -> p));
        List<PlaceSummaryDto> out = new ArrayList<>();
        for (Long id : ids) {
            Place p = map.get(id);
            if (p != null) {
                out.add(PlaceSummaryDto.from(p));
            }
        }
        return out;
    }
}
