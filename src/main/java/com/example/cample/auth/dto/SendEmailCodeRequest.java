package com.example.cample.auth.dto;

import com.example.cample.auth.domain.VerificationPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailCodeRequest(
        @NotBlank @Email String email,
        VerificationPurpose purpose
) {}
