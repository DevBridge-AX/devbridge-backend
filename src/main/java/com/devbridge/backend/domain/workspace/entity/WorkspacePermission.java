package com.devbridge.backend.domain.workspace.entity;

public enum WorkspacePermission {
    OWNER(3),
    MEMBER(2),
    GUEST(1);

    private final int level;

    WorkspacePermission(int level) {
        this.level = level;
    }

    public boolean hasAtLeast(WorkspacePermission required) {
        return this.level >= required.level;
    }
}
