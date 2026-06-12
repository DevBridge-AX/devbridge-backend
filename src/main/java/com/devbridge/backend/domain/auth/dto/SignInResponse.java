package com.devbridge.backend.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignInResponse {
    private String accessToken;
    private String tokenType;
    private String lastWorkspaceId;
}
