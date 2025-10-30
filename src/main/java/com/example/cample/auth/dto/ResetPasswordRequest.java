package com.example.cample.auth.dto;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min=4, max=10) String code,
        @NotBlank @Size(min=8, max=50) String newPassword
) {}
