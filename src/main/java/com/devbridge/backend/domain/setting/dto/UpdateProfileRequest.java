package com.devbridge.backend.domain.setting.dto;

import com.devbridge.backend.domain.user.entity.JobRole;

public record UpdateProfileRequest(
        String name,
        String department,
        String position,
        JobRole jobRole
) {}
