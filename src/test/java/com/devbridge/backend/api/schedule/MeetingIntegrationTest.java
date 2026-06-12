package com.devbridge.backend.api.schedule;

import com.devbridge.backend.domain.schedule.dto.*;
import com.devbridge.backend.domain.schedule.entity.MeetingStatus;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.entity.WorkspaceMember;
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
                .employeeId("EMP001")
                .email("julie019019@gmail.com")
                .name("김현수")
                .systemRole("USER")
                .authProvider("LOCAL")
                .build();
        userRepository.save(user1);

        User user2 = User.builder()
                .employeeId("EMP002")
                .email("user@company.com")
                .name("이원빈")
                .systemRole("USER")
                .authProvider("LOCAL")
                .build();
        userRepository.save(user2);

        // 워크스페이스 멤버 매핑 저장
        WorkspaceMember member1 = WorkspaceMember.builder()
                .workspace(savedWorkspace)
                .user(user1)
                .memberRole("OWNER")
                .joinedAt(LocalDateTime.now())
                .build();
        workspaceMemberRepository.save(member1);

        WorkspaceMember member2 = WorkspaceMember.builder()
                .workspace(savedWorkspace)
                .user(user2)
                .memberRole("MEMBER")
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
        // Step 1. 호스트(EMP001)가 타이틀과 대상을 정해 회의 생성 요청 (POST /api/meetings)
        CreateMeetingRequest createRequest = new CreateMeetingRequest(
                "API 설계 회고 미팅", 
                60, 
                List.of("EMP002") // 참여자로 EMP002 사번 지정 (총 2인 참여 미팅)
        );

        String createResponseJson = mockMvc.perform(post("/api/meetings")
                        .header("X-Workspace-Id", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest))
                        .with(authentication(getMockAuthentication("EMP001"))) // 호스트 사번 인증 주입
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meetingId").exists())
                .andReturn().getResponse().getContentAsString();

        String meetingId = objectMapper.readTree(createResponseJson).get("meetingId").asText();

        // Step 2. 생성자(호스트)인 EMP001이 참여 가능한 시간대 제출 (POST /api/meetings/{id}/participants/me/times)
        // 가능 시간대: 6월 15일 10:00 ~ 12:00
        SubmitAvailableTimesRequest hostTimes = new SubmitAvailableTimesRequest(List.of(
                new TimeSlotRequest(LocalDateTime.of(2026, 6, 15, 10, 0), LocalDateTime.of(2026, 6, 15, 12, 0))
        ));

        mockMvc.perform(post("/api/meetings/" + meetingId + "/participants/me/times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hostTimes))
                        .with(authentication(getMockAuthentication("EMP001")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allParticipantsResponded").value(false)) // 아직 EMP002가 제출하지 않아 false
                .andExpect(jsonPath("$.status").value("RESPONDED")); // status 필드로 응답 여부 확인 (이전 submit API 결과)

        // Step 3. 나머지 참여자 EMP002가 참여 가능한 시간대 제출 (POST /api/meetings/{id}/participants/me/times)
        // 가능 시간대: 6월 15일 09:00 ~ 11:30 (교집합 성립 범위: 10:00 ~ 11:30 = 90분, 회의 소요시간인 60분 충족)
        SubmitAvailableTimesRequest attendeeTimes = new SubmitAvailableTimesRequest(List.of(
                new TimeSlotRequest(LocalDateTime.of(2026, 6, 15, 9, 0), LocalDateTime.of(2026, 6, 15, 11, 30))
        ));

        mockMvc.perform(post("/api/meetings/" + meetingId + "/participants/me/times")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendeeTimes))
                        .with(authentication(getMockAuthentication("EMP002")))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allParticipantsResponded").value(true)) // 전원 제출 완료로 true
                .andExpect(jsonPath("$.status").value("RESPONDED"));

        // Step 4. 회의 상세 조회를 통해 백엔드가 교집합 최적 시간(10:00 ~ 11:00)으로 회의를 자동 확정했는지 검증 (GET /api/meetings/{id})
        mockMvc.perform(get("/api/meetings/" + meetingId)
                        .with(authentication(getMockAuthentication("EMP001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED")) // 회의 확정 상태 변경 검증
                .andExpect(jsonPath("$.confirmedStartTime").value("2026-06-15T10:00:00")) // 확정 시작 시각 확인
                .andExpect(jsonPath("$.confirmedEndTime").value("2026-06-15T11:00:00")) // 소요 시간인 60분이 더해진 확정 종료 시각 확인
                .andExpect(jsonPath("$.topCandidateTimes[0].startTime").value("2026-06-15T10:00:00")); // 후보군 데이터 정상 적재 확인

        // Step 5. 내 회의 목록 조회를 통해 내가 참여 중인 회의 목록이 정상 반환되는지 확인 (GET /api/meetings)
        mockMvc.perform(get("/api/meetings")
                        .with(authentication(getMockAuthentication("EMP001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meetingId").value(meetingId))
                .andExpect(jsonPath("$[0].title").value("API 설계 회고 미팅"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));

        // Step 6. 내 확정 일정 조회를 통해 특정 기간 내의 확정 일정 시간대가 정상 반환되는지 확인 (GET /api/meetings/participants/me/schedules)
        mockMvc.perform(get("/api/meetings/participants/me/schedules")
                        .param("startDate", "2026-06-15")
                        .param("endDate", "2026-06-15")
                        .with(authentication(getMockAuthentication("EMP001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].meetingId").value(meetingId))
                .andExpect(jsonPath("$[0].confirmedStartTime").value("2026-06-15T10:00:00"))
                .andExpect(jsonPath("$[0].confirmedEndTime").value("2026-06-15T11:00:00"));
    }
}
