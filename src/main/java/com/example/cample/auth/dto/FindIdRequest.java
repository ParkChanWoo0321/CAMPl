package com.example.cample.auth.dto;

import jakarta.validation.constraints.*;

public record FindIdRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min=4, max=10) String code
) {}
