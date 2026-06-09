package com.devbridge.backend.domain.setting.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        String name,
        String currentPassword,
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String newPassword
) {}
