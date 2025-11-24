// src/main/java/com/example/cample/place/domain/PlaceMenu.java
package com.example.cample.place.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "place_menus",
        indexes = {
                @Index(name = "idx_place_menu_place", columnList = "place_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 가게의 메뉴인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    // 메뉴 이름
    @Column(nullable = false, length = 100)
    private String name;

    // 가격 (원 단위)
    @Column(nullable = false)
    private int price;
}
