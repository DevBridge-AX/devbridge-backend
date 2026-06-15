package com.devbridge.backend.domain.schedule.dto;

import com.devbridge.backend.domain.schedule.entity.MeetingReference;
import com.devbridge.backend.domain.schedule.entity.ReferenceType;

import java.time.LocalDateTime;

public record MeetingReferenceResponse(
        String id,
        ReferenceType referenceType,
        String documentId,
        String fileUrl,
        String title,
        LocalDateTime createdAt
) {

    public static MeetingReferenceResponse from(MeetingReference reference) {
        return new MeetingReferenceResponse(
                reference.getId(),
                reference.getReferenceType(),
                reference.getDocument() != null ? reference.getDocument().getId() : null,
                reference.getFileUrl(),
                reference.getTitle(),
                reference.getCreatedAt());
    }
}
