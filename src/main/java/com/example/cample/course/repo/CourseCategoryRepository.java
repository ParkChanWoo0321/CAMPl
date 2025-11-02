package com.example.cample.course.repo;

import com.example.cample.course.domain.CourseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseCategoryRepository extends JpaRepository<CourseCategory, Long> {
    List<CourseCategory> findByParentIsNull();
    List<CourseCategory> findByParentId(Long parentId);
}
