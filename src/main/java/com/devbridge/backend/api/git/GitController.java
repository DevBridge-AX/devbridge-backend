package com.devbridge.backend.api.git;

import com.devbridge.backend.domain.git.dto.GitCommitResponse;
import com.devbridge.backend.domain.git.service.GitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GitController implements GitAPI {

    private final GitService gitService;

    @Override
    public ResponseEntity<List<GitCommitResponse>> getRecentCommits(int limit) {
        return ResponseEntity.ok(gitService.getRecentCommits(limit));
    }
}