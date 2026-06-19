package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.workspace.dto.WorkspaceMemberResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceResponse;
import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
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
    public List<WorkspaceResponse> getMyWorkspaces(String userId) {
        return workspaceMemberRepository.findAllWithWorkspaceByUserId(userId)
                .stream()
                .map(this::toWorkspaceResponse)
                .toList();
    }

    @Transactional
    public void updateLastAccessedAt(String userId, String workspaceId) {
        workspaceAccessService.updateLastAccessedAt(userId, workspaceId);
    }

    private WorkspaceResponse toWorkspaceResponse(WorkspaceMember workspaceMember) {
        return WorkspaceResponse.builder()
                .id(workspaceMember.getWorkspace().getId())
                .name(workspaceMember.getWorkspace().getName())
                .description(workspaceMember.getWorkspace().getDescription())
                .build();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> searchMembers(String workspaceId, String identifier, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        validateMembership(workspaceId, identifier);

        return workspaceMemberRepository.searchByWorkspaceIdAndUserNameContaining(workspaceId, identifier, keyword).stream()
                .map(member -> WorkspaceMemberResponse.builder()
                        .userId(member.getUser().getId())
                        .employeeId(member.getUser().getEmployeeId())
                        .name(member.getUser().getName())
                        .department(member.getUser().getDepartment())
                        .position(member.getUser().getPosition())
                        .permission(member.getPermission())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public void validateMembership(String workspaceId, String identifier) {
        if (!workspaceMemberRepository.existsByWorkspace_IdAndUser_EmployeeId(workspaceId, identifier) &&
                !workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspaceId, identifier)) {
            throw new IllegalArgumentException("해당 워크스페이스에 속한 사용자가 아닙니다.");
        }
    }

    @Transactional(readOnly = true)
    public void validatePermission(String workspaceId, String userId, String requiredPermission) {
        WorkspaceMember member = workspaceMemberRepository
                .findByUser_IdAndWorkspace_Id(userId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스에 속한 사용자가 아닙니다."));

        if (!requiredPermission.equals(member.getPermission())) {
            throw new IllegalArgumentException("해당 작업에 대한 권한이 없습니다.");
        }
    }
}