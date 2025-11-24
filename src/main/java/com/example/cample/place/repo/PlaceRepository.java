// src/main/java/com/example/cample/place/repo/PlaceRepository.java
package com.example.cample.place.repo;

import com.example.cample.place.domain.Place;
import com.example.cample.place.domain.PlaceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    // 타입별(식당/카페/술집) 전체 조회
    List<Place> findByType(PlaceType type);

    // 여러 타입 한 번에 조회 (예: RESTAURANT + CAFE)
    List<Place> findByTypeIn(List<PlaceType> types);
}
