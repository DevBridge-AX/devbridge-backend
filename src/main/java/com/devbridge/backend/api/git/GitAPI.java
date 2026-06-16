package com.devbridge.backend.api.git;

import com.devbridge.backend.domain.git.dto.GitCommitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Git", description = "Git API")
@RequestMapping("/api/git")
public interface GitAPI {

    @Operation(summary = "최근 Git Commit 조회", description = "로컬 Git 저장소의 최근 commit 목록을 조회합니다.")
    @GetMapping("/commits")
    ResponseEntity<List<GitCommitResponse>> getRecentCommits(
            @RequestParam(value = "limit", defaultValue = "20") int limit
    );
}