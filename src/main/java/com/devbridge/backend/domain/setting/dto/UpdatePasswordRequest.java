package com.devbridge.backend.domain.setting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수 입력 항목입니다.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호는 필수 입력 항목입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String newPassword
) {}
