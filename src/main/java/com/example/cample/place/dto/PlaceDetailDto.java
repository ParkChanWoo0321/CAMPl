// src/main/java/com/example/cample/place/dto/PlaceDetailDto.java
package com.example.cample.place.dto;

import com.example.cample.place.domain.Place;
import com.example.cample.place.domain.PlaceMenu;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceDetailDto {

    private Long id;
    private String name;
    private String imageUrl;
    private String address;
    private Double latitude;
    private Double longitude;

    private List<PlaceMenuDto> menus;

    public static PlaceDetailDto from(Place p, List<PlaceMenu> menuList) {
        return PlaceDetailDto.builder()
                .id(p.getId())
                .name(p.getName())
                .imageUrl(p.getImageUrl())
                .address(p.getAddress())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .menus(menuList.stream()
                        .map(PlaceMenuDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
