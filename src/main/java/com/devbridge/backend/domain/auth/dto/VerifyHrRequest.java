package com.devbridge.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyHrRequest(
        @NotBlank String employeeId,
        @NotBlank String name
) {}
