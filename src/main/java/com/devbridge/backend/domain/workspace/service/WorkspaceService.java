package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.workspace.dto.WorkspaceMemberResponse;
import com.devbridge.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> searchMembers(String workspaceId, String keyword) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new IllegalArgumentException("해당 워크스페이스가 존재하지 않습니다.");
        }

        return workspaceMemberRepository.searchByWorkspaceIdAndUserNameContaining(workspaceId, keyword).stream()
                .map(member -> WorkspaceMemberResponse.builder()
                        .userId(member.getUser().getId())
                        .employeeId(member.getUser().getEmployeeId())
                        .name(member.getUser().getName())
                        .department(member.getUser().getDepartment())
                        .position(member.getUser().getPosition())
                        .build())
                .toList();
    }
}
