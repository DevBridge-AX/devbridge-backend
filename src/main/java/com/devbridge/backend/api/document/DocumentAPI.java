package com.devbridge.backend.api.document;

import com.devbridge.backend.domain.document.dto.PresignedUrlRequest;
import com.devbridge.backend.domain.document.dto.PresignedUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Document", description = "문서 업로드용 Presigned URL 발급 API")
@RequestMapping("/api/documents")
public interface DocumentAPI {

    @Operation(summary = "파일 업로드용 Presigned URL 발급", description = "파일명과 콘텐츠 타입을 입력받아 업로드 URL, 파일 키, 파일 접근 URL을 발급합니다.")
    @PostMapping("/presigned-url")
    ResponseEntity<PresignedUrlResponse> getPresignedUrl(@Valid @RequestBody PresignedUrlRequest request);
}
