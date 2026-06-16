package com.devbridge.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record UserLookupResponse(
        @JsonProperty("user_id")
        @NotBlank
        String userId
) {}
