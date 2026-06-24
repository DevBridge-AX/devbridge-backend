package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.dto.CreateWorkspaceRequest;
import com.devbridge.backend.domain.workspace.dto.InviteMemberRequest;
import com.devbridge.backend.domain.workspace.dto.WorkspaceInvitationResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceMemberResponse;
import com.devbridge.backend.domain.workspace.dto.WorkspaceResponse;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.entity.WorkspaceInvitation;
import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
import com.devbridge.backend.domain.workspace.entity.WorkspacePermission;
import com.devbridge.backend.domain.workspace.repository.WorkspaceInvitationRepository;
import com.devbridge.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private static final String INVITATION_STATUS_PENDING = "PENDING";
    private static final String INVITATION_STATUS_ACCEPTED = "ACCEPTED";

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final WorkspaceContextValidator workspaceContextValidator;

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> getMyWorkspaces(String employeeId) {
        return workspaceMemberRepository.findAllWithWorkspaceByUserEmployeeId(employeeId)
                .stream()
                .map(this::toWorkspaceResponse)
                .toList();
    }

    @Transactional
    public WorkspaceResponse createWorkspace(String employeeId, CreateWorkspaceRequest request) {
        User creator = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        validateSystemAdmin(creator);
        validateWorkspaceCreateRequest(request);

        Workspace workspace = Workspace.builder()
                .name(request.getName().trim())
                .description(normalizeNullableText(request.getDescription()))
                .build();

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMember ownerMember = WorkspaceMember.builder()
                .workspace(savedWorkspace)
                .user(creator)
                .permission(WorkspacePermission.OWNER)
                .joinedAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();

        WorkspaceMember savedMember = workspaceMemberRepository.save(ownerMember);

        return toWorkspaceResponse(savedMember);
    }

    @Transactional
    public void inviteMember(String workspaceId, String inviterEmployeeId, InviteMemberRequest request) {
        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        String validWorkspaceId = workspace.getId();

        validatePermission(validWorkspaceId, inviterEmployeeId, WorkspacePermission.OWNER);
        validateInviteRequest(request);

        User inviter = userRepository.findByEmployeeId(inviterEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException("Inviter not found."));

        String invitedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
        WorkspacePermission assignedPermission = parseAssignablePermission(request.getRole());

        userRepository.findByEmail(invitedEmail).ifPresent(invitedUser -> {
            boolean alreadyMember = workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(
                    validWorkspaceId,
                    invitedUser.getId()
            );

            if (alreadyMember) {
                throw new IllegalArgumentException("The invited user is already a workspace member.");
            }
        });

        boolean alreadyPending = workspaceInvitationRepository.existsByWorkspace_IdAndInvitedEmailAndStatus(
                validWorkspaceId,
                invitedEmail,
                INVITATION_STATUS_PENDING
        );

        if (alreadyPending) {
            throw new IllegalArgumentException("A pending invitation already exists for this email.");
        }

        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
                .workspace(workspace)
                .invitedEmail(invitedEmail)
                .invitedBy(inviter)
                .assignedPermission(assignedPermission.name())
                .status(INVITATION_STATUS_PENDING)
                .build();

        workspaceInvitationRepository.save(invitation);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponse> getReceivedInvitations(String employeeId) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        return workspaceInvitationRepository.findReceivedInvitations(
                        user.getEmail(),
                        INVITATION_STATUS_PENDING
                )
                .stream()
                .map(this::toWorkspaceInvitationResponse)
                .toList();
    }

    @Transactional
    public void acceptInvitation(String employeeId, String invitationId) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        WorkspaceInvitation invitation = workspaceInvitationRepository.findWithWorkspaceById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found."));

        if (!INVITATION_STATUS_PENDING.equals(invitation.getStatus())) {
            throw new IllegalArgumentException("Only pending invitations can be accepted.");
        }

        if (!invitation.getInvitedEmail().equalsIgnoreCase(user.getEmail())) {
            throw new IllegalArgumentException("This invitation does not belong to the signed-in user.");
        }

        boolean alreadyMember = workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(
                invitation.getWorkspace().getId(),
                user.getId()
        );

        if (alreadyMember) {
            invitation.accept();
            return;
        }

        WorkspacePermission permission = parseAssignablePermission(invitation.getAssignedPermission());

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(invitation.getWorkspace())
                .user(user)
                .permission(permission)
                .joinedAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();

        workspaceMemberRepository.save(member);
        invitation.accept();
    }

    @Transactional
    public void updateLastAccessedAt(String employeeId, String workspaceId) {
        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        workspaceAccessService.updateLastAccessedAt(employeeId, workspace.getId());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> searchMembers(String workspaceId, String employeeId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        String validWorkspaceId = workspace.getId();

        validateMembership(validWorkspaceId, employeeId);

        return workspaceMemberRepository.searchByWorkspaceIdAndUserNameContaining(validWorkspaceId, employeeId, keyword)
                .stream()
                .map(member -> WorkspaceMemberResponse.builder()
                        .userId(member.getUser().getId())
                        .employeeId(member.getUser().getEmployeeId())
                        .name(member.getUser().getName())
                        .department(member.getUser().getDepartment())
                        .position(member.getUser().getPosition())
                        .permission(member.getPermission().name())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public void validateMembership(String workspaceId, String employeeId) {
        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        String validWorkspaceId = workspace.getId();

        if (!workspaceMemberRepository.existsByWorkspace_IdAndUser_EmployeeId(validWorkspaceId, employeeId)) {
            throw new IllegalArgumentException("The user is not a member of this workspace.");
        }
    }

    @Transactional(readOnly = true)
    public void validatePermission(String workspaceId, String employeeId, WorkspacePermission requiredPermission) {
        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);
        String validWorkspaceId = workspace.getId();

        WorkspaceMember member = workspaceMemberRepository
                .findByUser_EmployeeIdAndWorkspace_Id(employeeId, validWorkspaceId)
                .orElseThrow(() -> new IllegalArgumentException("The user is not a member of this workspace."));

        if (!member.getPermission().hasAtLeast(requiredPermission)) {
            throw new IllegalArgumentException("The user does not have permission for this operation.");
        }
    }

    private WorkspaceResponse toWorkspaceResponse(WorkspaceMember workspaceMember) {
        return WorkspaceResponse.builder()
                .id(workspaceMember.getWorkspace().getId())
                .name(workspaceMember.getWorkspace().getName())
                .description(workspaceMember.getWorkspace().getDescription())
                .myPermission(workspaceMember.getPermission().name())
                .build();
    }

    private WorkspaceInvitationResponse toWorkspaceInvitationResponse(WorkspaceInvitation invitation) {
        return WorkspaceInvitationResponse.builder()
                .invitationId(invitation.getId())
                .workspaceId(invitation.getWorkspace().getId())
                .workspaceName(invitation.getWorkspace().getName())
                .invitedEmail(invitation.getInvitedEmail())
                .assignedPermission(invitation.getAssignedPermission())
                .status(invitation.getStatus())
                .build();
    }

    private void validateSystemAdmin(User user) {
        String systemRole = user.getSystemRole();

        if (systemRole == null || !systemRole.toUpperCase(Locale.ROOT).contains("ADMIN")) {
            throw new IllegalArgumentException("Only admin users can create workspaces.");
        }
    }

    private void validateWorkspaceCreateRequest(CreateWorkspaceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateWorkspaceRequest is required.");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Workspace name is required.");
        }
    }

    private void validateInviteRequest(InviteMemberRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("InviteMemberRequest is required.");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Invited email is required.");
        }

        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new IllegalArgumentException("Assigned permission is required.");
        }
    }

    private WorkspacePermission parseAssignablePermission(String role) {
        WorkspacePermission permission;

        try {
            permission = WorkspacePermission.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid workspace permission. Allowed values are MEMBER or GUEST.");
        }

        if (permission == WorkspacePermission.OWNER) {
            throw new IllegalArgumentException("OWNER cannot be assigned through invitation.");
        }

        return permission;
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}