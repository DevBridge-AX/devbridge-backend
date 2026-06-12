package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
import com.devbridge.backend.domain.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WorkspaceAccessService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional(readOnly = true)
    public String findLastWorkspaceIdByUserId(String userId) {
        return workspaceMemberRepository
                .findRecentWorkspaceMembershipsByUserId(userId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(workspaceMember -> workspaceMember.getWorkspace().getId())
                .orElse(null);
    }

    @Transactional
    public void updateLastAccessedAt(String userId, String workspaceId) {
        WorkspaceMember workspaceMember = workspaceMemberRepository
                .findByUser_IdAndWorkspace_Id(userId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스 접근 권한이 없습니다."));

        workspaceMember.updateLastAccessedAt(LocalDateTime.now());
    }
}