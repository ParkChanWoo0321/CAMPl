// src/main/java/com/example/cample/timetable/dto/TryAddResponse.java
package com.example.cample.timetable.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TryAddResponse {
    private boolean conflict;     // true면 프런트 경고창 띄우기
    private Long itemId;          // conflict=false일 때 생성된 itemId
    private int createdEventCount; // conflict=false일 때 생성된 캘린더 이벤트 수
}
