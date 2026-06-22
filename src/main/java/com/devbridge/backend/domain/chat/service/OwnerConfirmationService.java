package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.entity.ChatMessage;
import com.devbridge.backend.domain.chat.entity.OwnerConfirmation;
import com.devbridge.backend.domain.chat.repository.ChatMessageRepository;
import com.devbridge.backend.domain.chat.repository.OwnerConfirmationRepository;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.notification.service.NotificationService;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .build();
        ownerConfirmationRepository.save(ownerConfirmation);

        notificationService.createNotification(owner, "OWNER_CONFIRMATION", ownerConfirmation.getId(),
                "담당자 확인 요청",
                "문서 근거가 부족한 질문이 배정되었습니다. 확인 후 답변해 주세요.");

        return Optional.of(owner.getName());
    }

    @Transactional
    public void createOwnerConfirmationFromChat(String messageId, String assignedOwnerId) {
        if (ownerConfirmationRepository.existsByQuestionMessage_IdAndStatus(messageId, "PENDING")) {
            throw new IllegalStateException("이미 담당자가 배정된 질문입니다.");
        }

        User owner = userRepository.findById(assignedOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자가 존재하지 않습니다: " + assignedOwnerId));

        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅 메시지가 존재하지 않습니다: " + messageId));

        OwnerConfirmation ownerConfirmation = OwnerConfirmation.builder()
                .workspace(chatMessage.getSession().getWorkspace())
                .questionMessage(chatMessage)
                .assignedOwner(owner)
                .build();
        ownerConfirmationRepository.save(ownerConfirmation);

        notificationService.createNotification(owner, "OWNER_CONFIRMATION", ownerConfirmation.getId(),
                "담당자 확인 요청",
                "채팅 질문에 대한 확인이 요청되었습니다. 답변해 주세요.");
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
                .build();
        ownerConfirmationRepository.save(ownerConfirmation);

        notificationService.createNotification(uploadedBy, "OWNER_CONFIRMATION", ownerConfirmation.getId(),
                "문서 관련 질문이 도착했습니다",
                requester.getName() + "님이 [" + document.getTitle() + "] 문서에 대해 질문을 남겼습니다.");
    }
}
