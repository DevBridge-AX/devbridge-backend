package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.ChatSessionResponse;
import com.devbridge.backend.domain.chat.dto.CreateChatSessionRequest;
import com.devbridge.backend.domain.chat.entity.ChatSession;
import com.devbridge.backend.domain.chat.repository.ChatSessionRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public ChatSessionResponse createSession(String workspaceId, String employeeId, CreateChatSessionRequest request) {
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. employeeId: " + employeeId));

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다. id: " + workspaceId));

        ChatSession session = ChatSession.builder()
                .workspace(workspace)
                .user(user)
                .sessionTitle(request.sessionTitle() != null ? request.sessionTitle() : "새로운 대화")
                .build();

        ChatSession saved = chatSessionRepository.save(session);

        return ChatSessionResponse.builder()
                .id(saved.getId())
                .workspaceId(workspace.getId())
                .employeeId(user.getEmployeeId())
                .sessionTitle(saved.getSessionTitle())
                .lastMessageAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getSessions(String workspaceId, String employeeId) {
        List<Object[]> results = chatSessionRepository.findSessionsSortedByLastMessage(workspaceId, employeeId);
        return results.stream()
                .map(row -> {
                    ChatSession session = (ChatSession) row[0];
                    LocalDateTime lastMessageAt = (LocalDateTime) row[1];
                    return ChatSessionResponse.builder()
                            .id(session.getId())
                            .workspaceId(session.getWorkspace().getId())
                            .employeeId(session.getUser().getEmployeeId())
                            .sessionTitle(session.getSessionTitle())
                            .lastMessageAt(lastMessageAt)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSession(String sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        chatSessionRepository.delete(session);
    }
}
