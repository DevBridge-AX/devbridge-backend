package com.devbridge.backend.api.document;

import com.devbridge.backend.domain.document.dto.PresignedUrlRequest;
import com.devbridge.backend.domain.document.dto.PresignedUrlResponse;
import com.devbridge.backend.domain.document.service.DocumentStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DocumentController implements DocumentAPI {

    private final DocumentStorageService documentStorageService;

    @Override
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(PresignedUrlRequest request) {
        return ResponseEntity.ok(documentStorageService.generatePresignedUrl(request.fileName(), request.contentType()));
    }
}
