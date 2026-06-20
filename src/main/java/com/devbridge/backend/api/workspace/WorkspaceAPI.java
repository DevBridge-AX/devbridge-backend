package com.devbridge.backend.api.workspace;

import com.devbridge.backend.domain.workspace.dto.CreateWorkspaceRequest;
import com.devbridge.backend.domain.workspace.dto.InviteMemberRequest;
import com.devbridge.backend.domain.workspace.dto.WorkspaceInvitationResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceMemberResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Workspace", description = "Workspace API")
@RequestMapping("/api/workspaces")
public interface WorkspaceAPI {

    @Operation(summary = "Create workspace", description = "Creates a new workspace.")
    @PostMapping
    ResponseEntity<WorkspaceResponse> createWorkspace(
            @AuthenticationPrincipal String employeeId,
            @RequestBody CreateWorkspaceRequest request
    );

    @Operation(summary = "Get my workspaces", description = "Gets workspaces joined by the signed-in user.")
    @GetMapping
    ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces(
            @AuthenticationPrincipal String employeeId
    );

    @Operation(summary = "Invite member", description = "Invites a user to a workspace.")
    @PostMapping("/invite")
    ResponseEntity<Void> inviteMember(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String employeeId,
            @RequestBody InviteMemberRequest request
    );

    @Operation(summary = "Get received invitations", description = "Gets pending workspace invitations for the signed-in user.")
    @GetMapping("/invitations/received")
    ResponseEntity<List<WorkspaceInvitationResponse>> getReceivedInvitations(
            @AuthenticationPrincipal String employeeId
    );

    @Operation(summary = "Accept invitation", description = "Accepts a pending workspace invitation.")
    @PostMapping("/invitations/{invitationId}/accept")
    ResponseEntity<Void> acceptInvitation(
            @AuthenticationPrincipal String employeeId,
            @PathVariable String invitationId
    );

    @Operation(summary = "Update workspace access time", description = "Updates the recent access time for a workspace.")
    @PatchMapping("/{workspaceId}/access")
    ResponseEntity<Void> updateWorkspaceAccess(
            @AuthenticationPrincipal String employeeId,
            @PathVariable String workspaceId
    );

    @Operation(summary = "Search workspace members", description = "Searches members in the current workspace by name.")
    @GetMapping("/members")
    ResponseEntity<List<WorkspaceMemberResponse>> searchMembers(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String employeeId,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword
    );
}