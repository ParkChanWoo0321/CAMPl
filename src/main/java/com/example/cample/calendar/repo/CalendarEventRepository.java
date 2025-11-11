// src/main/java/com/example/cample/calendar/repo/CalendarEventRepository.java
package com.example.cample.calendar.repo;

import com.example.cample.calendar.domain.CalendarEvent;
import com.example.cample.calendar.domain.EventType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("""
           select e
           from CalendarEvent e
           where e.startAt < :to
             and e.endAt   > :from
             and (
                    e.type = :school
                 or (e.ownerId = :ownerId and e.type in :ownerTypes)
                 )
           order by e.startAt asc, e.type asc
           """)
    List<CalendarEvent> findIntersectWithOwnerTypes(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("ownerId") Long ownerId,
            @Param("school") EventType school,
            @Param("ownerTypes") List<EventType> ownerTypes
    );

    Optional<CalendarEvent> findByIdAndOwnerId(Long id, Long ownerId);

    // 중요 일정 D-Day 용(개인 일정 기준)
    @Query("""
           select e
           from CalendarEvent e
           where e.ownerId = :ownerId
             and e.type = :type
             and e.important = true
             and e.startAt >= :now
           order by e.startAt asc
           """)
    List<CalendarEvent> findImportantUpcoming(
            @Param("ownerId") Long ownerId,
            @Param("type") EventType type,
            @Param("now") LocalDateTime now
    );
}
