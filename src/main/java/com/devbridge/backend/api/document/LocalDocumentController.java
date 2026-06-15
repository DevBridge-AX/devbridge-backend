package com.devbridge.backend.api.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Profile("local")
@RestController
public class LocalDocumentController implements LocalDocumentAPI {

    @Value("${document.upload-dir}")
    private String uploadDir;

    @Override
    public ResponseEntity<Void> upload(String fileKey, byte[] file) {
        Path target = resolveFilePath(fileKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, file);
        } catch (IOException e) {
            throw new IllegalStateException("파일 업로드에 실패했습니다.", e);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Resource> getFile(String fileKey) {
        Path target = resolveFilePath(fileKey);
        Resource resource = new FileSystemResource(target);
        if (!resource.exists()) {
            throw new IllegalArgumentException("파일을 찾을 수 없습니다.");
        }

        return ResponseEntity.ok()
                .contentType(resolveContentType(target))
                .body(resource);
    }

    private Path resolveFilePath(String fileKey) {
        Path baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = baseDir.resolve(fileKey).normalize();
        if (!target.startsWith(baseDir)) {
            throw new IllegalArgumentException("잘못된 파일 경로입니다.");
        }
        return target;
    }

    private MediaType resolveContentType(Path target) {
        try {
            String contentType = Files.probeContentType(target);
            return contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
