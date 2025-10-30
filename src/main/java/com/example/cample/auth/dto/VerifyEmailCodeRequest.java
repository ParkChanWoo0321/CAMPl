package com.example.cample.auth.dto;

import com.example.cample.auth.domain.VerificationPurpose;
import jakarta.validation.constraints.*;

public record VerifyEmailCodeRequest(
        @NotBlank @Email String email,
        VerificationPurpose purpose,
        @NotBlank @Size(min=4, max=10) String code
) {}
