package com.example.cample.timetable.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddResult {
    private Long itemId;
    private List<Long> removedItemIds;
    private int createdEventCount;
    private int deletedEventCount;
}
