package com.devbridge.backend.api.workspace;

import com.devbridge.backend.domain.workspace.dto.CreateWorkspaceRequest;
import com.devbridge.backend.domain.workspace.dto.InviteMemberRequest;
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
    public ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces(
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(workspaceService.getMyWorkspaces(userId));
    }

    @Override
    public ResponseEntity<WorkspaceResponse> createWorkspace(CreateWorkspaceRequest request) {
        WorkspaceResponse response = WorkspaceResponse.builder()
                .id("dummy-workspace-id")
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> inviteMember(InviteMemberRequest request) {
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> updateWorkspaceAccess(
            @AuthenticationPrincipal String userId,
            String workspaceId
    ) {
        workspaceService.updateLastAccessedAt(userId, workspaceId);
        return ResponseEntity.noContent().build();
    }
}