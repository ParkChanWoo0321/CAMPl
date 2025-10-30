package com.example.cample.auth.dto;

public record TokenResponse(
        String accessToken,
        Long id,
        String loginId,
        String name,
        String email,
        String provider
) {}
