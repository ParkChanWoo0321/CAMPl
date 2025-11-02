package com.example.cample.timetable.repo;

import com.example.cample.timetable.domain.TimetableCalendarMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableCalendarMapRepository extends JpaRepository<TimetableCalendarMap, Long> {
    List<TimetableCalendarMap> findByTimetableItemId(Long timetableItemId);
    void deleteByTimetableItemId(Long timetableItemId);
}
