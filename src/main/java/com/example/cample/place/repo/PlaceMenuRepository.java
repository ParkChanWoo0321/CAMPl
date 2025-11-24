// src/main/java/com/example/cample/place/repo/PlaceMenuRepository.java
package com.example.cample.place.repo;

import com.example.cample.place.domain.PlaceMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceMenuRepository extends JpaRepository<PlaceMenu, Long> {

    // 단일 장소의 메뉴 전체
    List<PlaceMenu> findByPlaceId(Long placeId);

    // 여러 장소의 메뉴 한 번에 (상세/리스트 최적화용)
    List<PlaceMenu> findByPlaceIdIn(List<Long> placeIds);
}
