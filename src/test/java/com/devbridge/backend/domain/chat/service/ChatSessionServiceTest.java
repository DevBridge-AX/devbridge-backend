package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.ChatSessionResponse;
import com.devbridge.backend.domain.chat.dto.CreateChatSessionRequest;
import com.devbridge.backend.domain.chat.entity.ChatSession;
import com.devbridge.backend.domain.chat.repository.ChatSessionRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    private ChatSessionService chatSessionService;

    @BeforeEach
    void setUp() {
        chatSessionService = new ChatSessionService(chatSessionRepository, userRepository, workspaceRepository);
    }

    @Test
    void createSession_성공적으로세션을생성한다() {
        User user = User.builder().id("user-1").employeeId("EMP001").build();
        Workspace workspace = Workspace.builder().id("workspace-1").build();
        CreateChatSessionRequest request = CreateChatSessionRequest.builder().sessionTitle("새로운 대화").build();

        ChatSession session = ChatSession.builder()
                .id("session-1")
                .workspace(workspace)
                .user(user)
                .sessionTitle("새로운 대화")
                .build();

        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(user));
        when(workspaceRepository.findById("workspace-1")).thenReturn(Optional.of(workspace));
        when(chatSessionRepository.save(any(ChatSession.class))).thenReturn(session);

        ChatSessionResponse response = chatSessionService.createSession("workspace-1", "EMP001", request);

        assertThat(response.id()).isEqualTo("session-1");
        assertThat(response.sessionTitle()).isEqualTo("새로운 대화");
        verify(chatSessionRepository, times(1)).save(any(ChatSession.class));
    }

    @Test
    void getSessions_최근메시지생성시간기준으로내림차순정렬된결과를반환한다() {
        User user = User.builder().id("user-1").employeeId("EMP001").build();
        Workspace workspace = Workspace.builder().id("workspace-1").build();

        ChatSession session1 = ChatSession.builder()
                .id("session-1")
                .workspace(workspace)
                .user(user)
                .sessionTitle("대화 1")
                .build();

        ChatSession session2 = ChatSession.builder()
                .id("session-2")
                .workspace(workspace)
                .user(user)
                .sessionTitle("대화 2")
                .build();

        LocalDateTime time1 = LocalDateTime.now();
        LocalDateTime time2 = LocalDateTime.now().minusHours(1);

        List<Object[]> queryResults = List.of(
                new Object[]{session1, time1},
                new Object[]{session2, time2}
        );

        when(chatSessionRepository.findSessionsSortedByLastMessage("workspace-1", "EMP001")).thenReturn(queryResults);

        List<ChatSessionResponse> responses = chatSessionService.getSessions("workspace-1", "EMP001");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo("session-1");
        assertThat(responses.get(0).lastMessageAt()).isEqualTo(time1);
        assertThat(responses.get(1).id()).isEqualTo("session-2");
        assertThat(responses.get(1).lastMessageAt()).isEqualTo(time2);
    }

    @Test
    void deleteSession_성공적으로세션을삭제한다() {
        ChatSession session = ChatSession.builder().id("session-1").build();
        when(chatSessionRepository.findById("session-1")).thenReturn(Optional.of(session));

        chatSessionService.deleteSession("session-1");

        verify(chatSessionRepository, times(1)).delete(session);
    }
}
