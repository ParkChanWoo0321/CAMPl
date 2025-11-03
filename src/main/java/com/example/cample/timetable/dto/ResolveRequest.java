// src/main/java/com/example/cample/timetable/dto/ResolveRequest.java
package com.example.cample.timetable.dto;

import com.example.cample.timetable.domain.ConflictResolution;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResolveRequest {
    @NotNull
    private Long courseId;

    @NotNull
    private ConflictResolution resolution; // KEEP | REPLACE
}
