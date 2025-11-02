package com.example.cample.course.repo;

import com.example.cample.course.domain.CourseTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseTimeRepository extends JpaRepository<CourseTime, Long> {
    List<CourseTime> findByCourseId(Long courseId);
    List<CourseTime> findByCourseIdIn(Collection<Long> course_id);
}
