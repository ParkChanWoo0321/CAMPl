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
                 or (e.type = :personal and e.ownerId = :ownerId)
                 )
           order by e.startAt asc, e.type asc
           """)
    List<CalendarEvent> findIntersect(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("ownerId") Long ownerId,
            @Param("school") EventType school,
            @Param("personal") EventType personal
    );

    Optional<CalendarEvent> findByIdAndOwnerId(Long id, Long ownerId);
}
