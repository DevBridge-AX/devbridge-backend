package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceContextValidator {

    private static final String INVALID_DUMMY_WORKSPACE_ID = "dummy-workspace-id";

    private final WorkspaceRepository workspaceRepository;

    public Workspace getValidWorkspace(String workspaceId) {
        validateWorkspaceIdFormat(workspaceId);

        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
    }

    public void validateWorkspaceExists(String workspaceId) {
        getValidWorkspace(workspaceId);
    }

    public void validateSameWorkspace(String expectedWorkspaceId, String actualWorkspaceId, String message) {
        validateWorkspaceIdFormat(expectedWorkspaceId);
        validateWorkspaceIdFormat(actualWorkspaceId);

        if (!expectedWorkspaceId.equals(actualWorkspaceId)) {
            throw new IllegalArgumentException(message
                    + " expectedWorkspaceId=" + expectedWorkspaceId
                    + ", actualWorkspaceId=" + actualWorkspaceId);
        }
    }

    public void validateWorkspaceIdFormat(String workspaceId) {
        if (workspaceId == null || workspaceId.isBlank()) {
            throw new IllegalArgumentException("Workspace ID is required.");
        }

        String normalizedWorkspaceId = workspaceId.trim();

        if (INVALID_DUMMY_WORKSPACE_ID.equals(normalizedWorkspaceId)) {
            throw new IllegalArgumentException("Invalid workspace ID: " + INVALID_DUMMY_WORKSPACE_ID);
        }
    }
}