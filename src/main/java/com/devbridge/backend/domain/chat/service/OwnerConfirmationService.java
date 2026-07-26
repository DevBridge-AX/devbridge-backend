package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.dto.OwnerConfirmationResponse;
import com.devbridge.backend.domain.chat.entity.ChatMessage;
import com.devbridge.backend.domain.chat.entity.OwnerConfirmation;
import com.devbridge.backend.domain.chat.repository.ChatMessageRepository;
import com.devbridge.backend.domain.chat.repository.OwnerConfirmationRepository;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.notification.service.NotificationService;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.service.WorkspaceContextValidator;
import com.devbridge.backend.domain.workspace.service.WorkspaceService;
import com.devbridge.backend.global.common.exception.ForbiddenException;
import com.devbridge.backend.global.config.fastapi.FastApiClient;
import com.devbridge.backend.global.config.websocket.WebSocketSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OwnerConfirmationService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final OwnerConfirmationRepository ownerConfirmationRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final NotificationService notificationService;
    private final FastApiClient fastApiClient;
    private final WebSocketSessionRegistry webSocketSessionRegistry;
    private final ObjectMapper objectMapper;
    private final WorkspaceContextValidator workspaceContextValidator;
    private final WorkspaceService workspaceService;

    /**
     * [보존 메서드 - 자동 트리거 재사용 용도]
     *
     * RAG 신뢰도 부족 시 Git 커밋 이력 기반으로 추천된 담당자(suggestedOwnerId)에게
     * 자동으로 OwnerConfirmation을 생성하고 알림을 전송하는 메서드.
     *
     * 현재는 사용자가 명시적으로 담당자를 선택하는 방식(createOwnerConfirmationFromChat)으로
     * 전환되어 직접 호출되는 곳이 없으나, 아래 케이스에서 재사용 가능:
     * - RAG 부족 후 일정 시간(예: 48시간) 내 사용자 미응답 시 자동 배정
     * - 시스템이 자동으로 담당자를 지정해야 하는 배치/스케줄러 케이스
     *
     * 재사용 시 참고:
     * - suggestedOwnerId: FastAPI done 이벤트의 getSuggestedOwnerId() (Git author UUID)
     * - 호출 전 동일 messageId로 이미 PENDING 상태 OwnerConfirmation이 있는지 중복 체크 필요
     *   (createOwnerConfirmationFromChat의 existsByQuestionMessage_IdAndStatus 참고)
     */
    @Transactional
    public Optional<String> triggerOwnerConfirmation(String messageId, String suggestedOwnerId) {
        User owner = userRepository.findById(suggestedOwnerId).orElse(null);
        if (owner == null) {
            log.warn("suggested_owner_id에 해당하는 사용자를 찾을 수 없습니다: {}", suggestedOwnerId);
            return Optional.empty();
        }

        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅 메시지가 존재하지 않습니다: " + messageId));

        OwnerConfirmation ownerConfirmation = OwnerConfirmation.builder()
                .workspace(chatMessage.getSession().getWorkspace())
                .questionMessage(chatMessage)
                .assignedOwner(owner)
                .requester(chatMessage.getSession().getUser())
                .build();
        ownerConfirmationRepository.save(ownerConfirmation);

        notificationService.createNotification(owner, "OWNER_CONFIRMATION", ownerConfirmation.getId(),
                "담당자 확인 요청",
                "문서 근거가 부족한 질문이 배정되었습니다. 확인 후 답변해 주세요.",
                chatMessage.getSession().getWorkspace().getId());

        return Optional.of(owner.getName());
    }

    @Transactional
    public void createOwnerConfirmationFromChat(String messageId, String assignedOwnerId,
                                                String requesterEmployeeId) {
        if (ownerConfirmationRepository.existsByQuestionMessage_IdAndStatus(messageId, "PENDING")) {
            throw new IllegalStateException("이미 담당자가 배정된 질문입니다.");
        }

        User owner = userRepository.findById(assignedOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다: " + assignedOwnerId));

        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅 메시지가 존재하지 않습니다: " + messageId));

        User requester = userRepository.findByEmployeeId(requesterEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다: " + requesterEmployeeId));

        OwnerConfirmation ownerConfirmation = OwnerConfirmation.builder()
                .workspace(chatMessage.getSession().getWorkspace())
                .questionMessage(chatMessage)
                .assignedOwner(owner)
                .requester(requester)
                .build();
        ownerConfirmationRepository.save(ownerConfirmation);

        notificationService.createNotification(owner, "OWNER_CONFIRMATION", ownerConfirmation.getId(),
                "담당자 확인 요청",
                "채팅 질문에 대한 확인이 요청되었습니다. 답변해 주세요.",
                chatMessage.getSession().getWorkspace().getId());
    }

    @Transactional
    public void createDirectQuestion(String workspaceId, String assignedOwnerId,
                                     String questionContent, String requesterEmployeeId) {
        Workspace workspace = workspaceContextValidator.getValidWorkspace(workspaceId);

        // workspaceId는 X-Workspace-Id 헤더로 전달되는 클라이언트 입력이므로 멤버십을 검증한다.
        workspaceService.validateMembership(workspace.getId(), requesterEmployeeId);

        User owner = userRepository.findById(assignedOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다: " + assignedOwnerId));

        User requester = userRepository.findByEmployeeId(requesterEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다: " + requesterEmployeeId));

        if (owner.getId().equals(requester.getId())) {
            throw new IllegalArgumentException("본인에게는 질문을 보낼 수 없습니다.");
        }

        OwnerConfirmation ownerConfirmation = OwnerConfirmation.builder()
                .workspace(workspace)
                .questionContent(questionContent)
                .assignedOwner(owner)
                .requester(requester)
                .build();
        ownerConfirmationRepository.save(ownerConfirmation);

        notificationService.createNotification(owner, "OWNER_CONFIRMATION_REQUESTED",
                ownerConfirmation.getId(),
                "담당자 질문이 도착했습니다",
                requester.getName() + "님이 질문을 남겼습니다: " + truncate(questionContent, 50),
                workspaceId);
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    @Transactional
    public void createOwnerConfirmationFromDocument(String documentId, String questionContent,
                                                    String requesterEmployeeId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 문서가 존재하지 않습니다: " + documentId));

        User uploadedBy = document.getUploadedBy();
        if (uploadedBy == null) {
            throw new IllegalArgumentException("등록자 정보가 없는 문서입니다. 담당자를 지정할 수 없습니다.");
        }

        User requester = userRepository.findByEmployeeId(requesterEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다: " + requesterEmployeeId));

        if (uploadedBy.getId().equals(requester.getId())) {
            throw new IllegalArgumentException("본인이 등록한 문서입니다.");
        }

        OwnerConfirmation ownerConfirmation = OwnerConfirmation.builder()
                .workspace(document.getDataSource().getWorkspace())
                .relatedDocument(document)
                .questionContent(questionContent)
                .assignedOwner(uploadedBy)
                .requester(requester)
                .build();
        ownerConfirmationRepository.save(ownerConfirmation);

        notificationService.createNotification(uploadedBy, "OWNER_CONFIRMATION", ownerConfirmation.getId(),
                "문서 관련 질문이 도착했습니다",
                requester.getName() + "님이 [" + document.getTitle() + "] 문서에 대해 질문을 남겼습니다.",
                document.getDataSource().getWorkspace().getId());
    }

    @Transactional
    public OwnerConfirmationResponse submitAnswer(String confirmationId, String ownerEmployeeId,
                                                  String answerContent) {
        OwnerConfirmation confirmation = ownerConfirmationRepository.findByIdAndDeletedAtIsNull(confirmationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 확인 요청이 존재하지 않습니다: " + confirmationId));

        if (!confirmation.getAssignedOwner().getEmployeeId().equals(ownerEmployeeId)) {
            throw new ForbiddenException("권한이 없습니다. 배정된 담당자만 답변할 수 있습니다.");
        }

        confirmation.submitAnswer(answerContent);

        User requester = confirmation.getRequester();
        if (requester != null) {
            String workspaceId = confirmation.getWorkspace().getId();

            notificationService.createNotification(requester, "OWNER_ANSWER_RECEIVED", confirmationId,
                    "담당자 답변이 도착했습니다",
                    confirmation.getAssignedOwner().getName() + "님이 질문에 답변했습니다.",
                    workspaceId);

            sendOwnerAnswerEvent(confirmation, requester);
        } else {
            log.warn("requester가 없는 확인 요청에 답변 완료 (레거시 행): confirmationId={}", confirmationId);
        }

        requestOwnerAnswerIngestion(confirmation, answerContent);

        return OwnerConfirmationResponse.from(confirmation);
    }

    @Transactional(readOnly = true)
    public OwnerConfirmationResponse getDetail(String confirmationId, String employeeId) {
        OwnerConfirmation confirmation = ownerConfirmationRepository.findByIdAndDeletedAtIsNull(confirmationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 확인 요청이 존재하지 않습니다: " + confirmationId));

        boolean isOwner = confirmation.getAssignedOwner().getEmployeeId().equals(employeeId);
        boolean isRequester = confirmation.getRequester() != null
                && confirmation.getRequester().getEmployeeId().equals(employeeId);

        if (!isOwner && !isRequester) {
            throw new ForbiddenException("해당 확인 요청에 대한 접근 권한이 없습니다.");
        }

        return OwnerConfirmationResponse.from(confirmation);
    }

    @Transactional(readOnly = true)
    public Page<OwnerConfirmationResponse> getAssignedConfirmations(String employeeId, String workspaceId,
                                                                    Pageable pageable) {
        workspaceService.validateMembership(workspaceId, employeeId);

        return ownerConfirmationRepository
                .findByAssignedOwner_EmployeeIdAndWorkspace_IdAndDeletedAtIsNull(employeeId, workspaceId, pageable)
                .map(OwnerConfirmationResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OwnerConfirmationResponse> getRequestedConfirmations(String employeeId, String workspaceId,
                                                                     Pageable pageable) {
        workspaceService.validateMembership(workspaceId, employeeId);

        return ownerConfirmationRepository
                .findByRequester_EmployeeIdAndWorkspace_IdAndDeletedAtIsNull(employeeId, workspaceId, pageable)
                .map(OwnerConfirmationResponse::from);
    }

    private void sendOwnerAnswerEvent(OwnerConfirmation confirmation, User requester) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "owner_answer_received");
            payload.put("original_message_id",
                    confirmation.getQuestionMessage() != null ? confirmation.getQuestionMessage().getId() : null);
            payload.put("confirmation_id", confirmation.getId());
            payload.put("content", confirmation.getAnswerContent());
            payload.put("owner_name", confirmation.getAssignedOwner().getName());
            payload.put("workspace_id", confirmation.getWorkspace().getId());

            webSocketSessionRegistry.sendToUser(
                    requester.getEmployeeId(), objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("답변 WebSocket push 실패: requesterEmployeeId={}, confirmationId={}, error={}",
                    requester.getEmployeeId(), confirmation.getId(), e.getMessage());
        }
    }

    private void requestOwnerAnswerIngestion(OwnerConfirmation confirmation, String answerContent) {
        String questionContent = confirmation.getQuestionContent();
        if (questionContent == null && confirmation.getQuestionMessage() != null) {
            questionContent = confirmation.getQuestionMessage().getContent();
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspace_id", confirmation.getWorkspace().getId());
        payload.put("confirmation_id", confirmation.getId());
        payload.put("question", questionContent);
        payload.put("answer", answerContent);
        payload.put("owner_employee_id", confirmation.getAssignedOwner().getEmployeeId());
        payload.put("owner_name", confirmation.getAssignedOwner().getName());

        fastApiClient.ingestOwnerAnswer(payload)
                .subscribe(
                        unused -> {},
                        e -> log.error("담당자 답변 벡터화 요청 실패: confirmationId={}, error={}",
                                confirmation.getId(), e.getMessage())
                );
    }
}
