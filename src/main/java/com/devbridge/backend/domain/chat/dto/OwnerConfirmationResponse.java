package com.devbridge.backend.domain.chat.dto;

import com.devbridge.backend.domain.chat.entity.OwnerConfirmation;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OwnerConfirmationResponse(
        String id,
        String workspaceId,
        String questionContent,
        String assignedOwnerEmployeeId,
        String assignedOwnerName,
        String requesterEmployeeId,
        String requesterName,
        String status,
        String answerContent,
        LocalDateTime answeredAt,
        LocalDateTime createdAt,
        String questionMessageId,
        String relatedDocumentId,
        String relatedDocumentTitle
) {
    public static OwnerConfirmationResponse from(OwnerConfirmation oc) {
        String questionContent = oc.getQuestionContent();
        if (questionContent == null && oc.getQuestionMessage() != null) {
            questionContent = oc.getQuestionMessage().getContent();
        }

        return OwnerConfirmationResponse.builder()
                .id(oc.getId())
                .workspaceId(oc.getWorkspace().getId())
                .questionContent(questionContent)
                .assignedOwnerEmployeeId(oc.getAssignedOwner().getEmployeeId())
                .assignedOwnerName(oc.getAssignedOwner().getName())
                .requesterEmployeeId(oc.getRequester() != null ? oc.getRequester().getEmployeeId() : null)
                .requesterName(oc.getRequester() != null ? oc.getRequester().getName() : null)
                .status(oc.getStatus())
                .answerContent(oc.getAnswerContent())
                .answeredAt(oc.getAnsweredAt())
                .createdAt(oc.getCreatedAt())
                .questionMessageId(oc.getQuestionMessage() != null ? oc.getQuestionMessage().getId() : null)
                .relatedDocumentId(oc.getRelatedDocument() != null ? oc.getRelatedDocument().getId() : null)
                .relatedDocumentTitle(oc.getRelatedDocument() != null ? oc.getRelatedDocument().getTitle() : null)
                .build();
    }
}
