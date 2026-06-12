package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.workspace.dto.WorkspaceMemberResponse;
import com.devbridge.backend.domain.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

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
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public void validateMembership(String workspaceId, String employeeId) {
        if (!workspaceMemberRepository.existsByWorkspace_IdAndUser_EmployeeId(workspaceId, employeeId)) {
            throw new IllegalArgumentException("해당 워크스페이스에 속한 사용자가 아닙니다.");
        }
    }
}
