package com.devbridge.backend.domain.document.dto;

public record PresignedUrlResponse(
        String uploadUrl,
        String fileKey,
        String fileUrl
) {
}
