package com.devbridge.backend.domain.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceInvitationResponse {
    private String invitationId;
    private String workspaceId;
    private String workspaceName;
    private String invitedEmail;
    private String assignedPermission;
    private String status;
}