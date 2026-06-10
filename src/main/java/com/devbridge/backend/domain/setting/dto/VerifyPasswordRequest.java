package com.devbridge.backend.domain.setting.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPasswordRequest(
        @NotBlank(message = "현재 비밀번호는 필수 입력 항목입니다.")
        String password
) {}
