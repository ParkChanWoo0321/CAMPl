// src/main/java/com/example/cample/place/dto/PlaceSummaryDto.java
package com.example.cample.place.dto;

import com.example.cample.place.domain.Place;
import com.example.cample.place.domain.PlaceType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceSummaryDto {

    private Long id;
    private String name;
    private PlaceType type;
    private String imageUrl;
    private String address;
    private Integer distanceMeters; // m 단위 거리(없으면 null)

    public static PlaceSummaryDto from(Place p) {
        return PlaceSummaryDto.builder()
                .id(p.getId())
                .name(p.getName())
                .type(p.getType())
                .imageUrl(p.getImageUrl())
                .address(p.getAddress())
                .distanceMeters(null)
                .build();
    }

    public static PlaceSummaryDto from(Place p, Integer distanceMeters) {
        return PlaceSummaryDto.builder()
                .id(p.getId())
                .name(p.getName())
                .type(p.getType())
                .imageUrl(p.getImageUrl())
                .address(p.getAddress())
                .distanceMeters(distanceMeters)
                .build();
    }
}
