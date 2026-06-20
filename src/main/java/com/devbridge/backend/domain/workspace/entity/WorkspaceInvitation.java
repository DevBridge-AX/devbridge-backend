package com.devbridge.backend.domain.workspace.entity;

import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "WORKSPACE_INVITATIONS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkspaceInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "invited_email", nullable = false, length = 255)
    private String invitedEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    // Temporary compatibility field.
    // Current DB has both assigned_role and assigned_permission.
    @Column(name = "assigned_role", nullable = false, length = 50)
    private String assignedRole;

    // Final target field. Later cleanup should keep this column only.
    @Column(name = "assigned_permission", nullable = false, length = 50)
    private String assignedPermission;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    public void accept() {
        this.status = "ACCEPTED";
    }
}