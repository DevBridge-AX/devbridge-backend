package com.devbridge.backend.api.workspace;

import com.devbridge.backend.domain.workspace.dto.CreateWorkspaceRequest;
import com.devbridge.backend.domain.workspace.dto.InviteMemberRequest;
import com.devbridge.backend.domain.workspace.dto.WorkspaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Workspace", description = "워크스페이스 API 명세")
@RequestMapping("/api/workspaces")
public interface WorkspaceAPI {

    @Operation(summary = "워크스페이스 생성", description = "새로운 워크스페이스(프로젝트 공간)를 생성합니다.")
    @PostMapping
    ResponseEntity<WorkspaceResponse> createWorkspace(@RequestBody CreateWorkspaceRequest request);

    @Operation(summary = "멤버 초대", description = "워크스페이스에 사용자를 멤버로 초대합니다.")
    @PostMapping("/invite")
    ResponseEntity<Void> inviteMember(@RequestBody InviteMemberRequest request);
}
