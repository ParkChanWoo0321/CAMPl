// src/main/java/com/example/cample/course/repo/CourseRepository.java
package com.example.cample.course.repo;

import com.example.cample.course.domain.Course;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    @Query("""
        select c from Course c
        where c.semesterCode = :semester
          and (:q is null or lower(c.name) like lower(concat('%', :q, '%'))
                        or lower(c.courseCode) like lower(concat('%', :q, '%')))
          and (:prof is null or lower(c.professor) like lower(concat('%', :prof, '%')))
          and (:categoryId is null or c.category.id = :categoryId)
          and (:year is null or c.year = :year)
    """)
    Page<Course> search(@Param("semester") String semester,
                        @Param("q") String q,
                        @Param("prof") String professor,
                        @Param("categoryId") Long categoryId,
                        @Param("year") Integer year,             // ← 추가
                        Pageable pageable);
}
