package com.devbridge.backend.domain.setting.dto;

public record ProfileResponse(
        String id,
        String employeeId,
        String name,
        String email,
        String department,
        String position
) {}
