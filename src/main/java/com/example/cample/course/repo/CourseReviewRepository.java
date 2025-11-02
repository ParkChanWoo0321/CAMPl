package com.example.cample.course.repo;

import com.example.cample.course.domain.CourseReview;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    @Query("""
        select r from CourseReview r
        where r.course.id = :courseId and r.deleted = false
        order by r.createdAt desc
    """)
    Page<CourseReview> findByCourseIdVisible(@Param("courseId") Long courseId, Pageable pageable);

    Optional<CourseReview> findByCourseIdAndUserId(Long courseId, Long userId);

    @Query("""
        select count(r), coalesce(avg(r.rating), 0)
        from CourseReview r
        where r.course.id = :courseId and r.deleted = false
    """)
    Object[] countAndAvg(@Param("courseId") Long courseId);
}
