package com.devbridge.backend.api.git;

import com.devbridge.backend.domain.git.dto.GitCommitResponse;
import com.devbridge.backend.domain.git.dto.GitIngestionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Git", description = "Git API")
@RequestMapping("/api/git")
public interface GitAPI {

    @Operation(
            summary = "최근 Git Commit 조회",
            description = "로컬 Git 저장소의 최근 commit 목록을 조회합니다."
    )
    @GetMapping("/commits")
    ResponseEntity<List<GitCommitResponse>> getRecentCommits(
            @RequestParam(value = "limit", defaultValue = "20") int limit
    );

    @Operation(
            summary = "Git Commit 변경 이력 수집",
            description = "Git 저장소에서 commit message와 diff를 수집하여 DB에 저장하고 AI Engine에 전달합니다."
    )
    @PostMapping("/ingest")
    ResponseEntity<Map<String, Object>> ingestGitCommits(
            @RequestBody GitIngestionRequest request
    );
}