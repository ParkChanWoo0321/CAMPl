package com.example.cample.timetable.repo;

import com.example.cample.timetable.domain.TimetableItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimetableItemRepository extends JpaRepository<TimetableItem, Long> {
    List<TimetableItem> findByUserIdAndSemesterCode(Long userId, String semesterCode);
    Optional<TimetableItem> findByUserIdAndSemesterCodeAndCourseId(Long userId, String semesterCode, Long courseId);
}
