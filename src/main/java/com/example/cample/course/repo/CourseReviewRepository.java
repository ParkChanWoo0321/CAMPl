// src/main/java/com/example/cample/course/repo/CourseReviewRepository.java
package com.example.cample.course.repo;

import com.example.cample.course.domain.CourseReview;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    Optional<CourseReview> findByCourseIdAndUserId(Long courseId, Long userId);

    // 상세 조회에서 사용할 가시(visible) 리뷰 전체 목록
    List<CourseReview> findByCourseIdAndDeletedFalseOrderByCreatedAtDesc(Long courseId);

    // 평점 요약(평균/개수)
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
