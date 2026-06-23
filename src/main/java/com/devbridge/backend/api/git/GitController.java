package com.devbridge.backend.api.git;

import com.devbridge.backend.domain.git.dto.GitCommitDetail;
import com.devbridge.backend.domain.git.dto.GitCommitResponse;
import com.devbridge.backend.domain.git.dto.GitIngestionRequest;
import com.devbridge.backend.domain.git.service.GitIngestionService;
import com.devbridge.backend.domain.git.service.GitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class GitController implements GitAPI {

    private final GitService gitService;
    private final GitIngestionService gitIngestionService;

    @Override
    public ResponseEntity<List<GitCommitResponse>> getRecentCommits(int limit) {
        return ResponseEntity.ok(gitService.getRecentCommits(limit));
    }

    @Override
    public ResponseEntity<Map<String, Object>> ingestGitCommits(GitIngestionRequest request) {
        List<GitCommitDetail> commitDetails = gitIngestionService.ingestGitCommits(request);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("commitCount", commitDetails.size());
        response.put("commits", commitDetails.stream()
                .map(this::toSummary)
                .toList());

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> toSummary(GitCommitDetail commit) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("hash", commit.getHash());
        summary.put("shortHash", commit.getShortHash());
        summary.put("message", commit.getMessage());
        summary.put("branchName", commit.getBranchName());
        summary.put("changedFileCount", commit.getChangedFiles() == null ? 0 : commit.getChangedFiles().size());
        return summary;
    }
}