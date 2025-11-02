package com.example.cample.course.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReviewRequest {
    @NotNull @DecimalMin("1.0") @DecimalMax("5.0")
    private Double rating;

    @Size(max = 2000)
    private String content;
}
