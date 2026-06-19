package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.workspace.dto.WorkspaceMemberResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceResponse;
import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
import com.devbridge.backend.domain.workspace.entity.WorkspacePermission;
import com.devbridge.backend.domain.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceAccessService workspaceAccessService;

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMyWorkspaces(String employeeId) {
        return workspaceMemberRepository.findAllWithWorkspaceByUserEmployeeId(employeeId)
                .stream()
                .map(this::toWorkspaceResponse)
                .toList();
    }

    @Transactional
    public void updateLastAccessedAt(String employeeId, String workspaceId) {
        workspaceAccessService.updateLastAccessedAt(employeeId, workspaceId);
    }

    private WorkspaceResponse toWorkspaceResponse(WorkspaceMember workspaceMember) {
        return WorkspaceResponse.builder()
                .id(workspaceMember.getWorkspace().getId())
                .name(workspaceMember.getWorkspace().getName())
                .description(workspaceMember.getWorkspace().getDescription())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> searchMembers(String workspaceId, String employeeId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        validateMembership(workspaceId, employeeId);

        return workspaceMemberRepository.searchByWorkspaceIdAndUserNameContaining(workspaceId, employeeId, keyword).stream()
                .map(member -> WorkspaceMemberResponse.builder()
                        .userId(member.getUser().getId())
                        .employeeId(member.getUser().getEmployeeId())
                        .name(member.getUser().getName())
                        .department(member.getUser().getDepartment())
                        .position(member.getUser().getPosition())
                        .permission(member.getPermission().name())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public void validateMembership(String workspaceId, String employeeId) {
        if (!workspaceMemberRepository.existsByWorkspace_IdAndUser_EmployeeId(workspaceId, employeeId)) {
            throw new IllegalArgumentException("해당 워크스페이스에 속한 사용자가 아닙니다.");
        }
    }

    @Transactional(readOnly = true)
    public void validatePermission(String workspaceId, String employeeId, WorkspacePermission requiredPermission) {
        WorkspaceMember member = workspaceMemberRepository
                .findByUser_EmployeeIdAndWorkspace_Id(employeeId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스에 속한 사용자가 아닙니다."));

        if (!member.getPermission().hasAtLeast(requiredPermission)) {
            throw new IllegalArgumentException("해당 작업에 대한 권한이 없습니다.");
        }
    }
}