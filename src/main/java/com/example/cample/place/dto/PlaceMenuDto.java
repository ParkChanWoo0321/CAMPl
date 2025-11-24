// src/main/java/com/example/cample/place/dto/PlaceMenuDto.java
package com.example.cample.place.dto;

import com.example.cample.place.domain.PlaceMenu;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceMenuDto {

    private Long id;
    private String name;
    private int price;

    public static PlaceMenuDto from(PlaceMenu m) {
        return PlaceMenuDto.builder()
                .id(m.getId())
                .name(m.getName())
                .price(m.getPrice())
                .build();
    }
}
