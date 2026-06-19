-- ============================================================
-- Migration: WorkspaceInvitation의 assigned_role → assigned_permission 리네임
-- Date: 2026-06-19
-- ============================================================

ALTER TABLE WORKSPACE_INVITATIONS
    RENAME COLUMN assigned_role TO assigned_permission;
