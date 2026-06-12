package com.devbridge.backend.domain.setting.dto;


public record UpdateProfileRequest(
        String name,
        String department,
        String position
) {}
