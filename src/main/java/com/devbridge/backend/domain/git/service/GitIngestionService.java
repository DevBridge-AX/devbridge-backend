package com.devbridge.backend.domain.git.service;

import com.devbridge.backend.domain.datasource.entity.DataSource;
import com.devbridge.backend.domain.datasource.entity.GitCommit;
import com.devbridge.backend.domain.datasource.entity.GitCommitAnalysis;
import com.devbridge.backend.domain.datasource.entity.GitCommitFile;
import com.devbridge.backend.domain.datasource.repository.DataSourceRepository;
import com.devbridge.backend.domain.datasource.repository.GitCommitAnalysisRepository;
import com.devbridge.backend.domain.datasource.repository.GitCommitFileRepository;
import com.devbridge.backend.domain.datasource.repository.GitCommitRepository;
import com.devbridge.backend.domain.git.dto.GitChangedFileDetail;
import com.devbridge.backend.domain.git.dto.GitCommitDetail;
import com.devbridge.backend.domain.git.dto.GitIngestionRequest;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import com.devbridge.backend.global.config.fastapi.FastApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitIngestionService {

    private final GitService gitService;
    private final WorkspaceRepository workspaceRepository;
    private final DataSourceRepository dataSourceRepository;
    private final GitCommitRepository gitCommitRepository;
    private final GitCommitFileRepository gitCommitFileRepository;
    private final GitCommitAnalysisRepository gitCommitAnalysisRepository;
    private final FastApiClient fastApiClient;

    @Transactional
    public List<GitCommitDetail> ingestGitCommits(GitIngestionRequest request) {
        validateRequest(request);

        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + request.getWorkspaceId()));

        DataSource dataSource = dataSourceRepository.findById(request.getDataSourceId())
                .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + request.getDataSourceId()));

        validateGitDataSource(workspace, dataSource);

        List<GitCommitDetail> commitDetails = gitService.collectCommitDetails(
                request.getRepositoryPath(),
                request.getLimit()
        );

        for (GitCommitDetail commitDetail : commitDetails) {
            saveCommitIfAbsent(workspace, dataSource, commitDetail);
        }

        triggerAiEngineGitIngestion(workspace.getId(), dataSource.getId(), commitDetails);

        return commitDetails;
    }

    private void validateRequest(GitIngestionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Git ingestion request is required.");
        }

        if (request.getWorkspaceId() == null || request.getWorkspaceId().isBlank()) {
            throw new IllegalArgumentException("Workspace ID is required.");
        }

        if (request.getDataSourceId() == null || request.getDataSourceId().isBlank()) {
            throw new IllegalArgumentException("DataSource ID is required.");
        }
    }

    private void validateGitDataSource(Workspace workspace, DataSource dataSource) {
        String workspaceId = workspace.getId();
        String dataSourceWorkspaceId = dataSource.getWorkspace().getId();

        if (!workspaceId.equals(dataSourceWorkspaceId)) {
            throw new IllegalArgumentException("DataSource does not belong to workspace.");
        }

        if (!"GIT".equalsIgnoreCase(dataSource.getSourceType())) {
            throw new IllegalArgumentException("DataSource is not a GIT source.");
        }
    }

    private GitCommit saveCommitIfAbsent(
            Workspace workspace,
            DataSource dataSource,
            GitCommitDetail commitDetail
    ) {
        return gitCommitRepository.findByWorkspace_IdAndCommitHash(workspace.getId(), commitDetail.getHash())
                .orElseGet(() -> {
                    GitCommit savedCommit = gitCommitRepository.save(GitCommit.builder()
                            .workspace(workspace)
                            .dataSource(dataSource)
                            .authorName(blankToNull(commitDetail.getAuthorName()))
                            .authorEmail(blankToNull(commitDetail.getAuthorEmail()))
                            .commitHash(requiredText(commitDetail.getHash(), "commit hash"))
                            .shortHash(blankToNull(commitDetail.getShortHash()))
                            .commitMessage(defaultText(commitDetail.getMessage(), "(no commit message)"))
                            .branchName(blankToNull(commitDetail.getBranchName()))
                            .pushedAt(defaultTime(commitDetail.getCommittedAt()))
                            .build());

                    saveChangedFiles(savedCommit, commitDetail.getChangedFiles());
                    createPendingAnalysis(savedCommit, workspace);

                    return savedCommit;
                });
    }

    private void saveChangedFiles(GitCommit gitCommit, List<GitChangedFileDetail> changedFiles) {
        if (changedFiles == null || changedFiles.isEmpty()) {
            return;
        }

        for (GitChangedFileDetail changedFile : changedFiles) {
            gitCommitFileRepository.save(GitCommitFile.builder()
                    .gitCommit(gitCommit)
                    .filePath(defaultText(changedFile.getFilePath(), "unknown"))
                    .changeType(defaultText(changedFile.getChangeType(), "MODIFIED"))
                    .additions(changedFile.getAdditions())
                    .deletions(changedFile.getDeletions())
                    .patch(blankToNull(changedFile.getPatch()))
                    .diffSummary(blankToNull(changedFile.getDiffSummary()))
                    .build());
        }
    }

    private void createPendingAnalysis(GitCommit gitCommit, Workspace workspace) {
        if (gitCommitAnalysisRepository.findByGitCommit_Id(gitCommit.getId()).isPresent()) {
            return;
        }

        gitCommitAnalysisRepository.save(GitCommitAnalysis.builder()
                .gitCommit(gitCommit)
                .workspace(workspace)
                .summary(null)
                .impactArea(null)
                .riskLevel(null)
                .nextAction(null)
                .vectorId(null)
                .indexStatus("PENDING")
                .analyzedAt(null)
                .build());
    }

    private void triggerAiEngineGitIngestion(
            String workspaceId,
            String dataSourceId,
            List<GitCommitDetail> commitDetails
    ) {
        Map<String, Object> payload = buildGitIngestionPayload(workspaceId, dataSourceId, commitDetails);

        fastApiClient.ingestGit(payload)
                .subscribe(
                        unused -> log.info("Git ingestion request sent to AI Engine. workspaceId={}, commitCount={}",
                                workspaceId, commitDetails.size()),
                        e -> log.error("Failed to request Git ingestion to AI Engine. workspaceId={}, error={}",
                                workspaceId, e.getMessage())
                );
    }

    private Map<String, Object> buildGitIngestionPayload(
            String workspaceId,
            String dataSourceId,
            List<GitCommitDetail> commitDetails
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspace_id", workspaceId);
        payload.put("data_source_id", dataSourceId);
        payload.put("commits", commitDetails.stream()
                .map(this::toGitCommitPayload)
                .collect(Collectors.toList()));

        return payload;
    }

    private Map<String, Object> toGitCommitPayload(GitCommitDetail commitDetail) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commit_hash", commitDetail.getHash());
        payload.put("short_hash", commitDetail.getShortHash());
        payload.put("author_name", commitDetail.getAuthorName());
        payload.put("author_email", commitDetail.getAuthorEmail());
        payload.put("committed_at", commitDetail.getCommittedAt() == null ? null : commitDetail.getCommittedAt().toString());
        payload.put("message", commitDetail.getMessage());
        payload.put("branch_name", commitDetail.getBranchName());
        payload.put("changed_files", commitDetail.getChangedFiles() == null
                ? List.of()
                : commitDetail.getChangedFiles().stream()
                        .map(this::toGitChangedFilePayload)
                        .collect(Collectors.toList()));

        return payload;
    }

    private Map<String, Object> toGitChangedFilePayload(GitChangedFileDetail changedFile) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("file_path", changedFile.getFilePath());
        payload.put("change_type", changedFile.getChangeType());
        payload.put("additions", changedFile.getAdditions());
        payload.put("deletions", changedFile.getDeletions());
        payload.put("patch", changedFile.getPatch());
        payload.put("diff_summary", changedFile.getDiffSummary());

        return payload;
    }

    private String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        return value;
    }

    private String defaultText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

    private LocalDateTime defaultTime(LocalDateTime value) {
        return value == null ? LocalDateTime.now() : value;
    }
}