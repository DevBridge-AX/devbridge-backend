package com.devbridge.backend.domain.chat.service;

import com.devbridge.backend.domain.chat.entity.ChatMessage;
import com.devbridge.backend.domain.chat.entity.OwnerConfirmation;
import com.devbridge.backend.domain.chat.repository.ChatMessageRepository;
import com.devbridge.backend.domain.chat.repository.OwnerConfirmationRepository;
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
    private final NotificationService notificationService;

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
}
