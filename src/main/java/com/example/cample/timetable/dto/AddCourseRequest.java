// src/main/java/com/example/cample/timetable/dto/AddCourseRequest.java
package com.example.cample.timetable.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddCourseRequest {
    @NotNull
    private Long courseId;
}
