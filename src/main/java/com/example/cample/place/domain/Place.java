// src/main/java/com/example/cample/place/domain/Place.java
package com.example.cample.place.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "places",
        indexes = {
                @Index(name = "idx_place_type", columnList = "type"),
                @Index(name = "idx_place_name", columnList = "name")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 식당/카페/술집 이름
    @Column(nullable = false, length = 100)
    private String name;

    // RESTAURANT / CAFE / BAR
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PlaceType type;

    // 대표 이미지 URL (리스트/상세에서 공통 사용)
    @Column(length = 500)
    private String imageUrl;

    // 주소 (선택)
    @Column
    private String address;

    // 지도 표시용 좌표 (선택)
    @Column
    private Double latitude;   // 위도

    @Column
    private Double longitude;  // 경도

    // 메뉴 목록
    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlaceMenu> menus = new ArrayList<>();
}
