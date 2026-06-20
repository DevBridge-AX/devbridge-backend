package com.devbridge.backend.api.workspace;

import com.devbridge.backend.domain.workspace.dto.CreateWorkspaceRequest;
import com.devbridge.backend.domain.workspace.dto.InviteMemberRequest;
import com.devbridge.backend.domain.workspace.dto.WorkspaceInvitationResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceMemberResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceResponse;
import com.devbridge.backend.domain.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WorkspaceController implements WorkspaceAPI {

    private final WorkspaceService workspaceService;

    @Override
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @AuthenticationPrincipal String employeeId,
            CreateWorkspaceRequest request
    ) {
        return ResponseEntity.ok(workspaceService.createWorkspace(employeeId, request));
    }

    @Override
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces(
            @AuthenticationPrincipal String employeeId
    ) {
        return ResponseEntity.ok(workspaceService.getMyWorkspaces(employeeId));
    }

    @Override
    public ResponseEntity<Void> inviteMember(
            String workspaceId,
            @AuthenticationPrincipal String employeeId,
            InviteMemberRequest request
    ) {
        workspaceService.inviteMember(workspaceId, employeeId, request);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<WorkspaceInvitationResponse>> getReceivedInvitations(
            @AuthenticationPrincipal String employeeId
    ) {
        return ResponseEntity.ok(workspaceService.getReceivedInvitations(employeeId));
    }

    @Override
    public ResponseEntity<Void> acceptInvitation(
            @AuthenticationPrincipal String employeeId,
            String invitationId
    ) {
        workspaceService.acceptInvitation(employeeId, invitationId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateWorkspaceAccess(
            @AuthenticationPrincipal String employeeId,
            String workspaceId
    ) {
        workspaceService.updateLastAccessedAt(employeeId, workspaceId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<WorkspaceMemberResponse>> searchMembers(
            String workspaceId,
            String employeeId,
            String keyword
    ) {
        return ResponseEntity.ok(workspaceService.searchMembers(workspaceId, employeeId, keyword));
    }
}