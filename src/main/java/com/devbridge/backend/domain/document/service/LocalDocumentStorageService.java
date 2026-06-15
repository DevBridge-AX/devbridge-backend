package com.devbridge.backend.domain.document.service;

import com.devbridge.backend.domain.document.dto.PresignedUrlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Profile("local")
@Service
public class LocalDocumentStorageService implements DocumentStorageService {

    @Value("${document.base-url}")
    private String baseUrl;

    @Override
    public PresignedUrlResponse generatePresignedUrl(String originalFileName, String contentType) {
        String fileKey = UUID.randomUUID() + "_" + sanitizeFileName(originalFileName);
        String uploadUrl = baseUrl + "/upload/" + fileKey;
        String fileUrl = baseUrl + "/" + fileKey;

        return new PresignedUrlResponse(uploadUrl, fileKey, fileUrl);
    }

    private String sanitizeFileName(String originalFileName) {
        return originalFileName.replaceAll("[/\\\\]", "_");
    }
}
