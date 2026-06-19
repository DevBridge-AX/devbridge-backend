package com.devbridge.backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private String id;
    private String employeeId;
    private String email;
    private String name;
    private String department;
    private String position;
    private String jobRole;
    private String systemRole;
}
