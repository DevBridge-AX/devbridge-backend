package com.devbridge.backend.domain.workspace.service;

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
}