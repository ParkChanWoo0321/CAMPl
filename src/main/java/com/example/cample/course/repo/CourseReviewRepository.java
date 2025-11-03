// src/main/java/com/example/cample/course/repo/CourseReviewRepository.java
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

    // ✅ Object[] 대신 인터페이스 프로젝션 사용
    interface CountAvg {
        Long getCnt();
        Double getAvg();
    }

    @Query("""
        select count(r) as cnt, coalesce(avg(r.rating), 0.0d) as avg
        from CourseReview r
        where r.course.id = :courseId and r.deleted = false
    """)
    CountAvg countAndAvg(@Param("courseId") Long courseId);
}
