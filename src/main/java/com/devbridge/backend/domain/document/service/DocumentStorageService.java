package com.devbridge.backend.domain.document.service;

import com.devbridge.backend.domain.document.dto.PresignedUrlResponse;

public interface DocumentStorageService {

    PresignedUrlResponse generatePresignedUrl(String originalFileName, String contentType);
}
