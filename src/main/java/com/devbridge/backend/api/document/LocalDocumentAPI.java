package com.devbridge.backend.api.document;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "LocalDocument", description = "로컬 디스크 기반 파일 업로드/서빙 API (local 프로필)")
@RequestMapping("/documents")
public interface LocalDocumentAPI {

    @Operation(summary = "파일 업로드", description = "Presigned URL 발급 시 받은 업로드 URL로 파일 원본을 업로드합니다.")
    @PutMapping("/upload/{fileKey}")
    ResponseEntity<Void> upload(@PathVariable("fileKey") String fileKey, @RequestBody byte[] file);

    @Operation(summary = "파일 조회", description = "업로드된 파일을 fileKey로 조회하여 반환합니다.")
    @GetMapping("/{fileKey}")
    ResponseEntity<Resource> getFile(@PathVariable("fileKey") String fileKey);
}
