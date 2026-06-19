package com.devbridge.backend.api.workspace;

import com.devbridge.backend.domain.workspace.dto.CreateWorkspaceRequest;
import com.devbridge.backend.domain.workspace.dto.InviteMemberRequest;
import com.devbridge.backend.domain.workspace.dto.WorkspaceMemberResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Workspace", description = "Workspace API")
@RequestMapping("/api/workspaces")
public interface WorkspaceAPI {

    @Operation(summary = "Workspace 생성", description = "새 Workspace를 생성합니다.")
    @PostMapping
    ResponseEntity<WorkspaceResponse> createWorkspace(@RequestBody CreateWorkspaceRequest request);

    @Operation(summary = "내 Workspace 목록 조회", description = "로그인 사용자가 참여 중인 Workspace 목록을 조회합니다.")
    @GetMapping
    ResponseEntity<List<WorkspaceResponse>> getMyWorkspaces(
            @AuthenticationPrincipal String employeeId
    );

    @Operation(summary = "멤버 초대", description = "워크스페이스에 사용자를 멤버로 초대합니다.")
    @PostMapping("/invite")
    ResponseEntity<Void> inviteMember(@RequestBody InviteMemberRequest request);

    @Operation(summary = "Workspace 최근 접속 시간 갱신", description = "사용자가 Workspace에 진입하거나 선택했을 때 최근 접속 시간을 갱신합니다.")
    @PatchMapping("/{workspaceId}/access")
    ResponseEntity<Void> updateWorkspaceAccess(
            @AuthenticationPrincipal String employeeId,
            @PathVariable String workspaceId
    );

    @Operation(summary = "워크스페이스 멤버 검색", description = "현재 워크스페이스에 속한 멤버를 이름으로 검색합니다. 로그인한 사용자 본인은 결과에서 제외되며, 검색어가 없으면 빈 목록을 반환합니다. 회의 참석자 지정 등에서 사용됩니다.")
    @GetMapping("/members")
    ResponseEntity<List<WorkspaceMemberResponse>> searchMembers(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String employeeId,
            @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword);
}