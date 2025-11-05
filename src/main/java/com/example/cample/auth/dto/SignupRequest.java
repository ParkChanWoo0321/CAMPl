// src/main/java/com/example/cample/auth/dto/SignupRequest.java
package com.example.cample.auth.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank @Pattern(regexp="^[a-zA-Z0-9_\\-]{4,20}$", message="4~20자 영문/숫자/_-") String loginId,
        @NotBlank @Email String email,
        @NotBlank @Size(min=8, max=50) String password,
        @NotBlank String passwordConfirm,
        @NotBlank @Size(min=4, max=10) String code
) {}
