package com.example.cample.course.dto;

import com.example.cample.course.domain.CourseCategory;
import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryNodeDto {
    private Long id;
    private String name;
    private List<CategoryNodeDto> children;

    public static CategoryNodeDto from(CourseCategory c, List<CategoryNodeDto> children) {
        return CategoryNodeDto.builder()
                .id(c.getId())
                .name(c.getName())
                .children(children)
                .build();
    }
}
