package com.devbridge.backend.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailRequest(
        @NotBlank String employeeId,
        @NotBlank @Email String email
) {}
