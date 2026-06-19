package com.devbridge.backend.global.config;

import com.devbridge.backend.domain.schedule.entity.Meeting;
import com.devbridge.backend.domain.schedule.entity.MeetingParticipant;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import com.devbridge.backend.domain.schedule.entity.ParticipantRole;
import com.devbridge.backend.domain.schedule.entity.ParticipantStatus;
import com.devbridge.backend.domain.schedule.repository.MeetingParticipantRepository;
import com.devbridge.backend.domain.schedule.repository.MeetingRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
import com.devbridge.backend.domain.workspace.entity.WorkspacePermission;
import com.devbridge.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class MeetingDataInitializer implements ApplicationRunner {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initializing mockup meeting data...");

        Workspace ws001 = workspaceRepository.findById("ws001").orElse(null);
        Workspace dummyWs = workspaceRepository.findById("dummy-workspace-id").orElse(null);

        User host = userRepository.findByEmployeeId("EMP003").orElse(null);
        User attendee = userRepository.findByEmployeeId("EMP004").orElse(null);

        // Ensure all users are workspace members
        if (ws001 != null) {
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                ensureWorkspaceMembership(ws001, u);
            }
        }
        if (dummyWs != null) {
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                ensureWorkspaceMembership(dummyWs, u);
            }
        }

        if (host == null || attendee == null) {
            log.warn("Mockup users EMP003 or EMP004 not found in database. Skipping meeting mockup initialization.");
            return;
        }

        if (ws001 != null) {
            initializeMeetingsForWorkspace(ws001, host, attendee);
        }
        if (dummyWs != null) {
            initializeMeetingsForWorkspace(dummyWs, host, attendee);
        }

        log.info("Mockup meeting data successfully initialized.");
    }

    private void initializeMeetingsForWorkspace(Workspace workspace, User host, User attendee) {
        String title = "[목업] 파일 업로드 테스트용 회의";
        if (!meetingRepository.existsByWorkspace_IdAndTitle(workspace.getId(), title)) {
            // Create a gathering meeting
            Meeting gatheringMeeting = Meeting.builder()
                    .workspace(workspace)
                    .title(title)
                    .durationMinutes(60)
                    .status(MeetingStatus.GATHERING)
                    .build();
            meetingRepository.save(gatheringMeeting);

            MeetingParticipant hostPart = MeetingParticipant.builder()
                    .meeting(gatheringMeeting)
                    .employeeId(host.getEmployeeId())
                    .status(ParticipantStatus.PENDING)
                    .role(ParticipantRole.HOST)
                    .build();

            MeetingParticipant attendeePart = MeetingParticipant.builder()
                    .meeting(gatheringMeeting)
                    .employeeId(attendee.getEmployeeId())
                    .status(ParticipantStatus.PENDING)
                    .role(ParticipantRole.ATTENDEE)
                    .build();

            meetingParticipantRepository.saveAll(List.of(hostPart, attendeePart));
            log.info("Mockup gathering meeting created for workspace {}", workspace.getId());
        }

        // Create a confirmed meeting as well for testing confirmed schedules lookup
        String confirmedTitle = "[목업] 확정된 파일 업로드 테스트용 회의";
        if (!meetingRepository.existsByWorkspace_IdAndTitle(workspace.getId(), confirmedTitle)) {
            Meeting confirmedMeeting = Meeting.builder()
                    .workspace(workspace)
                    .title(confirmedTitle)
                    .durationMinutes(60)
                    .status(MeetingStatus.CONFIRMED)
                    .build();
            
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            confirmedMeeting.confirmSchedule(now.plusDays(1), now.plusDays(1).plusHours(1));
            meetingRepository.save(confirmedMeeting);

            MeetingParticipant hostConf = MeetingParticipant.builder()
                    .meeting(confirmedMeeting)
                    .employeeId(host.getEmployeeId())
                    .status(ParticipantStatus.RESPONDED)
                    .role(ParticipantRole.HOST)
                    .build();

            MeetingParticipant attendeeConf = MeetingParticipant.builder()
                    .meeting(confirmedMeeting)
                    .employeeId(attendee.getEmployeeId())
                    .status(ParticipantStatus.RESPONDED)
                    .role(ParticipantRole.ATTENDEE)
                    .build();

            meetingParticipantRepository.saveAll(List.of(hostConf, attendeeConf));
            log.info("Mockup confirmed meeting created for workspace {}", workspace.getId());
        }
    }

    private void ensureWorkspaceMembership(Workspace workspace, User user) {
        if (!workspaceMemberRepository.existsByWorkspace_IdAndUser_Id(workspace.getId(), user.getId())) {
            WorkspaceMember member = WorkspaceMember.builder()
                    .workspace(workspace)
                    .user(user)
                    .permission(WorkspacePermission.MEMBER)
                    .joinedAt(java.time.LocalDateTime.now())
                    .build();
            workspaceMemberRepository.save(member);
            log.info("Automatically added user {} to workspace {}", user.getEmployeeId(), workspace.getId());
        }
    }
}
