package com.example.cample.timetable.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "timetable_calendar_map",
        indexes = {
                @Index(name = "idx_tcm_item", columnList = "timetable_item_id")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimetableCalendarMap {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timetable_item_id", nullable = false)
    private Long timetableItemId;

    @Column(name = "calendar_event_id", nullable = false)
    private Long calendarEventId;
}
