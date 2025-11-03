// src/main/java/com/example/cample/course/repo/CourseRepository.java
package com.example.cample.course.repo;

import com.example.cample.course.domain.Course;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    @Query("""
        select distinct c from Course c
        left join CourseTime t on t.course = c
        where c.semesterCode = :semester
          and (:categoryId is null or c.category.id = :categoryId)
          and (:credit    is null or c.credit = :credit)
          and (:year      is null or c.year   = :year)
          and (
                (:start is null and :end is null)
             or (:start is not null and :end is null and t.startTime <= :start and t.endTime > :start)
             or (:start is null and :end is not null and t.startTime < :end and t.endTime >= :end)
             or (:start is not null and :end is not null and t.startTime < :end and t.endTime > :start)
          )
          and (:name is null or lower(c.name)       like lower(concat('%', :name, '%')))
          and (:prof is null or lower(c.professor)  like lower(concat('%', :prof, '%')))
          and (:code is null or lower(c.courseCode) like lower(concat('%', :code, '%')))
          and (:room is null or lower(t.room)       like lower(concat('%', :room, '%')))
    """)
    List<Course> searchAll(@Param("semester") String semester,
                           @Param("categoryId") Long categoryId,
                           @Param("credit") Integer credit,
                           @Param("year") String year,
                           @Param("start") LocalTime start,
                           @Param("end") LocalTime end,
                           @Param("name") String name,
                           @Param("prof") String professor,
                           @Param("code") String courseCode,
                           @Param("room") String room);
}
