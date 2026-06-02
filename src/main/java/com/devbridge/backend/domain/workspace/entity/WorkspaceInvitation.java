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
    private String id; // 초대 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace; // 워크스페이스 ID

    @Column(name = "invited_email", nullable = false, length = 255)
    private String invitedEmail; // 초대 대상 (미가입자)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy; // 초대 발송자

    @Column(name = "assigned_role", nullable = false, length = 50)
    private String assignedRole; // 부여할 권한

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, ACCEPTED
}
