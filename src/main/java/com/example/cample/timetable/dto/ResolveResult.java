// src/main/java/com/example/cample/timetable/dto/ResolveResult.java
package com.example.cample.timetable.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResolveResult {
    private boolean applied;          // REPLACE로 실제 변경/추가가 일어났는지
    private Long itemId;              // 새로 추가된 항목 ID (있으면)
    private List<Long> removedItemIds;// 대체로 삭제된 기존 항목들
    private int createdEventCount;    // 생성된 이벤트 수
    private int deletedEventCount;    // 삭제된 이벤트 수
}
