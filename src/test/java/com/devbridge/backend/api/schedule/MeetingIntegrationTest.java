package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.*;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
import com.devbridge.backend.domain.workspace.entity.WorkspacePermission;
import com.devbridge.backend.domain.workspace.repository.WorkspaceMemberRepository;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MeetingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    private String workspaceId;
    private String user2Id;

    @BeforeEach
    void setUp() {
        // H2 DB 상에 테스트를 위한 기초 워크스페이스 저장
        Workspace workspace = Workspace.builder()
                .name("테스트 워크스페이스")
                .description("API 통합 테스트용 워크스페이스")
                .build();
        Workspace savedWorkspace = workspaceRepository.save(workspace);
        this.workspaceId = savedWorkspace.getId();

        // 사용자 저장
        User user1 = User.builder()
                .employeeId("EMP003")
                .email("choie000208@gmail.com")
                .name("최형수")
                .systemRole("USER")
                .authProvider("LOCAL")
                .build();
        userRepository.save(user1);

        User user2 = User.builder()
                .employeeId("EMP004")
                .email("sam000208@naver.com")
                .name("최펭수")
                .systemRole("USER")
                .authProvider("LOCAL")
                .build();
        this.user2Id = userRepository.save(user2).getId();

        // 워크스페이스 멤버 매핑 저장
        WorkspaceMember member1 = WorkspaceMember.builder()
                .workspace(savedWorkspace)
                .user(user1)
                .permission(WorkspacePermission.OWNER)
                .joinedAt(LocalDateTime.now())
                .build();
        workspaceMemberRepository.save(member1);

        WorkspaceMember member2 = WorkspaceMember.builder()
                .workspace(savedWorkspace)
                .user(user2)
                .permission(WorkspacePermission.MEMBER)
                .joinedAt(LocalDateTime.now())
                .build();
        workspaceMemberRepository.save(member2);
    }

    private UsernamePasswordAuthenticationToken getMockAuthentication(String employeeId) {
        return new UsernamePasswordAuthenticationToken(employeeId, "mock-token", List.of());
    }

    @Test
    @DisplayName("회의 방 생성부터 참여자들의 일정 제출 후 자동 스케줄링 및 확정 처리까지의 전체 비즈니스 시나리오 테스트")
    void meeting_creation_scheduling_and_confirmation_flow() throws Exception {
        // Step 1. 호스트(EMP003)가 타이틀과 대상을 정해 회의 생성 요청 (POST /api/meetings)
        CreateMeetingRequest createRequest = new CreateMeetingRequest(
                "API 설계 회고 미팅",
                "API 설계 회고",
                "지난 스프린트 API 설계 리뷰",
                "회의실 A",
                60,
                List.of("EMP004"), // 참여자로 EMP004 사번 지정 (총 2인 참여 미팅)
                null
        );

        String createResponseJson = mockMvc.perform(post("/api/meetings")
                        .header("X-Workspace-Id", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(authentication(getMockAuthentication("EMP003"))) // 호스트 사번 인증 주입
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingId").exists())
                .andReturn().getResponse().getContentAsString();

        String meetingId = objectMapper.readTree(createResponseJson).get("meetingId").asText();

        // Step 2. 생성자(호스트)인 EMP003이 참여 가능한 시간대 제출 (POST /api/meetings/{id}/participants/me/times)
        // 가능 시간대: 6월 15일 10:00 ~ 12:00
        SubmitAvailableTimesRequest hostTimes = new SubmitAvailableTimesRequest(List.of(
                new TimeSlotRequest(LocalDateTime.of(2026, 6, 15, 10, 0), LocalDateTime.of(2026, 6, 15, 12, 0))
        ));

        mockMvc.perform(post("/api/meetings/" + meetingId + "/participants/me/times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostTimes))
                        .with(authentication(getMockAuthentication("EMP003")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allParticipantsResponded").value(false)) // 아직 EMP004가 제출하지 않아 false
                .andExpect(jsonPath("$.status").value("RESPONDED")); // status 필드로 응답 여부 확인 (이전 submit API 결과)

        // Step 3. 나머지 참여자 EMP004가 참여 가능한 시간대 제출 (POST /api/meetings/{id}/participants/me/times)
        // 가능 시간대: 6월 15일 09:00 ~ 11:30 (교집합 성립 범위: 10:00 ~ 11:30 = 90분, 회의 소요시간인 60분 충족)
        SubmitAvailableTimesRequest attendeeTimes = new SubmitAvailableTimesRequest(List.of(
                new TimeSlotRequest(LocalDateTime.of(2026, 6, 15, 9, 0), LocalDateTime.of(2026, 6, 15, 11, 30))
        ));

        mockMvc.perform(post("/api/meetings/" + meetingId + "/participants/me/times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendeeTimes))
                        .with(authentication(getMockAuthentication("EMP004")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allParticipantsResponded").value(true)) // 전원 제출 완료로 true
                .andExpect(jsonPath("$.status").value("RESPONDED"));

        // Step 4. 회의 상세 조회를 통해 백엔드가 교집합 최적 시간(10:00 ~ 11:00)으로 회의를 자동 확정했는지 검증 (GET /api/meetings/{id})
        mockMvc.perform(get("/api/meetings/" + meetingId)
                        .with(authentication(getMockAuthentication("EMP003"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED")) // 회의 확정 상태 변경 검증
                .andExpect(jsonPath("$.confirmedStartTime").value("2026-06-15T10:00:00")) // 확정 시작 시각 확인
                .andExpect(jsonPath("$.confirmedEndTime").value("2026-06-15T11:00:00")) // 소요 시간인 60분이 더해진 확정 종료 시각 확인
                .andExpect(jsonPath("$.topCandidateTimes[0].startTime").value("2026-06-15T10:00:00")); // 후보군 데이터 정상 적재 확인

        // Step 5. 내 회의 목록 조회를 통해 내가 참여 중인 회의 목록이 정상 반환되는지 확인 (GET /api/meetings)
        mockMvc.perform(get("/api/meetings")
                        .header("X-Workspace-Id", workspaceId)
                        .with(authentication(getMockAuthentication("EMP003"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meetingId").value(meetingId))
                .andExpect(jsonPath("$[0].title").value("API 설계 회고 미팅"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        // Step 6. 내 확정 일정 조회를 통해 특정 기간 내의 확정 일정 시간대가 정상 반환되는지 확인 (GET /api/meetings/participants/me/schedules)
        mockMvc.perform(get("/api/meetings/participants/me/schedules")
                        .header("X-Workspace-Id", workspaceId)
                        .param("startDate", "2026-06-15")
                        .param("endDate", "2026-06-15")
                        .with(authentication(getMockAuthentication("EMP003"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meetingId").value(meetingId))
                .andExpect(jsonPath("$[0].confirmedStartTime").value("2026-06-15T10:00:00"))
                .andExpect(jsonPath("$[0].confirmedEndTime").value("2026-06-15T11:00:00"));

        // Step 7. 주최자가 아닌 EMP004가 회의 정보 수정을 시도하면 거부됨 (PATCH /api/meetings/{id})
        UpdateMeetingRequest updateRequest = new UpdateMeetingRequest("API 설계 회고 미팅(수정)", "API 설계 회고(수정)", "지난 스프린트 API 설계 리뷰 및 차기 계획", "회의실 B", null);

        mockMvc.perform(patch("/api/meetings/" + meetingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(authentication(getMockAuthentication("EMP004")))
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        // Step 8. 주최자(EMP003)가 회의 정보를 수정하면 정상 반영됨 (PATCH /api/meetings/{id})
        mockMvc.perform(patch("/api/meetings/" + meetingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                        .with(authentication(getMockAuthentication("EMP003")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("API 설계 회고 미팅(수정)"))
                .andExpect(jsonPath("$.purpose").value("API 설계 회고(수정)"))
                .andExpect(jsonPath("$.agenda").value("지난 스프린트 API 설계 리뷰 및 차기 계획"))
                .andExpect(jsonPath("$.location").value("회의실 B"));

        // Step 9. 회의 정보 수정으로 인해 다른 참석자(EMP004)에게 알림이 전달되었는지 확인 (GET /api/notifications/users/{employeeId})
        mockMvc.perform(get("/api/notifications/users/EMP004")
                        .with(authentication(getMockAuthentication("EMP004"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].notificationType").value("MEETING_UPDATED"))
                .andExpect(jsonPath("$.content[0].referenceId").value(meetingId));
    }
}
