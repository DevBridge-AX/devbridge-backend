package com.devbridge.backend.api.workspace;

import com.devbridge.backend.domain.workspace.dto.CreateWorkspaceRequest;
import com.devbridge.backend.domain.workspace.dto.InviteMemberRequest;
import com.devbridge.backend.domain.workspace.dto.WorkspaceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WorkspaceController implements WorkspaceAPI {

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
}
