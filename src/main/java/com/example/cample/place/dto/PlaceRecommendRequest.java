// src/main/java/com/example/cample/place/dto/PlaceRecommendRequest.java
package com.example.cample.place.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceRecommendRequest {

    // 예: "TEAM_PROJECT", "STUDY", "MEAL", "MEETING", "REST" 등
    private String scheduleType;

    // 예: "ALONE", "FRIEND", "SENIOR", "JUNIOR", "PROFESSOR" 등
    private String withWhom;
}
