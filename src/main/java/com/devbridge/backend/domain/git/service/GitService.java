package com.devbridge.backend.domain.git.service;

import com.devbridge.backend.domain.git.dto.GitChangedFileDetail;
import com.devbridge.backend.domain.git.dto.GitCommitDetail;
import com.devbridge.backend.domain.git.dto.GitCommitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitService {

    private static final DateTimeFormatter GIT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${devbridge.git.repository-path:.}")
    private String repositoryPath;

    public List<GitCommitResponse> getRecentCommits(int limit) {
        int safeLimit = normalizeLimit(limit);
        String effectiveRepositoryPath = resolveRepositoryPath(null);
        String branchName = getCurrentBranchName(effectiveRepositoryPath);

        List<String> lines = executeGitLog(effectiveRepositoryPath, safeLimit);
        List<GitCommitResponse> commits = new ArrayList<>();

        for (String line : lines) {
            GitCommitResponse commit = parseGitLogLine(line, branchName);

            if (commit != null) {
                commits.add(commit);
            }
        }

        return commits;
    }

    public List<GitCommitDetail> collectCommitDetails(String requestedRepositoryPath, int limit) {
        int safeLimit = normalizeLimit(limit);
        String effectiveRepositoryPath = resolveRepositoryPath(requestedRepositoryPath);
        String branchName = getCurrentBranchName(effectiveRepositoryPath);

        List<String> lines = executeGitLog(effectiveRepositoryPath, safeLimit);
        List<GitCommitDetail> commits = new ArrayList<>();

        for (String line : lines) {
            GitCommitDetail commit = parseGitCommitDetailLine(line, branchName, effectiveRepositoryPath);

            if (commit != null) {
                commits.add(commit);
            }
        }

        return commits;
    }

    private int normalizeLimit(int limit) {
        return limit <= 0 ? 20 : Math.min(limit, 100);
    }

    private String resolveRepositoryPath(String requestedRepositoryPath) {
        String path = requestedRepositoryPath;

        if (path == null || path.isBlank()) {
            path = repositoryPath;
        }

        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Git repository path is required.");
        }

        File repositoryDirectory = new File(path);

        if (!repositoryDirectory.exists() || !repositoryDirectory.isDirectory()) {
            throw new IllegalArgumentException("Git repository path does not exist: " + path);
        }

        File gitDirectory = new File(repositoryDirectory, ".git");

        if (!gitDirectory.exists()) {
            throw new IllegalArgumentException("Path is not a Git repository: " + path);
        }

        return repositoryDirectory.getAbsolutePath();
    }

    private List<String> executeGitLog(String repositoryPath, int limit) {
        return executeGitCommandLines(
                repositoryPath,
                List.of(
                        "git",
                        "log",
                        "--pretty=format:%H%x1f%h%x1f%an%x1f%ae%x1f%ad%x1f%s",
                        "--date=format:%Y-%m-%d %H:%M:%S",
                        "-n",
                        String.valueOf(limit)
                )
        );
    }

    private GitCommitResponse parseGitLogLine(String line, String branchName) {
        String[] parts = line.split("\u001f", -1);

        if (parts.length < 6) {
            return null;
        }

        return GitCommitResponse.builder()
                .hash(parts[0])
                .shortHash(parts[1])
                .authorName(parts[2])
                .authorEmail(parts[3])
                .committedAt(parts[4])
                .message(parts[5])
                .branchName(branchName)
                .build();
    }

    private GitCommitDetail parseGitCommitDetailLine(
            String line,
            String branchName,
            String repositoryPath
    ) {
        String[] parts = line.split("\u001f", -1);

        if (parts.length < 6) {
            return null;
        }

        String commitHash = parts[0];

        return GitCommitDetail.builder()
                .hash(commitHash)
                .shortHash(parts[1])
                .authorName(parts[2])
                .authorEmail(parts[3])
                .committedAt(parseCommittedAt(parts[4]))
                .message(parts[5])
                .branchName(branchName)
                .changedFiles(collectChangedFiles(repositoryPath, commitHash))
                .build();
    }

    private LocalDateTime parseCommittedAt(String value) {
        try {
            return LocalDateTime.parse(value, GIT_DATE_FORMATTER);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Git committedAt: " + value, e);
        }
    }

    private List<GitChangedFileDetail> collectChangedFiles(String repositoryPath, String commitHash) {
        Map<String, String> changeTypeByFilePath = collectChangeTypes(repositoryPath, commitHash);
        List<String> numstatLines = executeGitCommandLines(
                repositoryPath,
                List.of(
                        "git",
                        "show",
                        "--numstat",
                        "--format=",
                        commitHash
                )
        );

        List<GitChangedFileDetail> changedFiles = new ArrayList<>();

        for (String line : numstatLines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\t", 3);

            if (parts.length < 3) {
                continue;
            }

            String filePath = parts[2];
            Integer additions = parseNullableInt(parts[0]);
            Integer deletions = parseNullableInt(parts[1]);
            String changeType = changeTypeByFilePath.getOrDefault(filePath, "MODIFIED");
            String patch = collectFilePatch(repositoryPath, commitHash, filePath);

            changedFiles.add(GitChangedFileDetail.builder()
                    .filePath(filePath)
                    .changeType(changeType)
                    .additions(additions)
                    .deletions(deletions)
                    .patch(patch)
                    .diffSummary(buildDiffSummary(changeType, filePath, additions, deletions))
                    .build());
        }

        return changedFiles;
    }

    private Map<String, String> collectChangeTypes(String repositoryPath, String commitHash) {
        List<String> statusLines = executeGitCommandLines(
                repositoryPath,
                List.of(
                        "git",
                        "show",
                        "--name-status",
                        "--format=",
                        commitHash
                )
        );

        Map<String, String> result = new HashMap<>();

        for (String line : statusLines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\t");

            if (parts.length < 2) {
                continue;
            }

            String rawStatus = parts[0];
            String changeType = mapGitChangeType(rawStatus);

            if (rawStatus.startsWith("R") && parts.length >= 3) {
                result.put(parts[2], changeType);
            } else {
                result.put(parts[1], changeType);
            }
        }

        return result;
    }

    private String mapGitChangeType(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "MODIFIED";
        }

        if (rawStatus.startsWith("A")) {
            return "ADDED";
        }

        if (rawStatus.startsWith("D")) {
            return "DELETED";
        }

        if (rawStatus.startsWith("R")) {
            return "RENAMED";
        }

        if (rawStatus.startsWith("C")) {
            return "COPIED";
        }

        return "MODIFIED";
    }

    private Integer parseNullableInt(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String collectFilePatch(String repositoryPath, String commitHash, String filePath) {
        return executeGitCommandText(
                repositoryPath,
                List.of(
                        "git",
                        "show",
                        "--format=",
                        "--patch",
                        commitHash,
                        "--",
                        filePath
                )
        );
    }

    private String buildDiffSummary(
            String changeType,
            String filePath,
            Integer additions,
            Integer deletions
    ) {
        String additionsText = additions == null ? "unknown additions" : additions + " additions";
        String deletionsText = deletions == null ? "unknown deletions" : deletions + " deletions";

        return changeType + " " + filePath + " (" + additionsText + ", " + deletionsText + ")";
    }

    private String getCurrentBranchName(String repositoryPath) {
        try {
            List<String> lines = executeGitCommandLines(
                    repositoryPath,
                    List.of(
                            "git",
                            "branch",
                            "--show-current"
                    )
            );

            if (lines.isEmpty() || lines.get(0).isBlank()) {
                return "unknown";
            }

            return lines.get(0).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private List<String> executeGitCommandLines(String repositoryPath, List<String> command) {
        String output = executeGitCommandText(repositoryPath, command);
        List<String> lines = new ArrayList<>();

        for (String line : output.split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }

        return lines;
    }

    private String executeGitCommandText(String repositoryPath, List<String> command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(repositoryPath));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException(
                        "Failed to execute Git command. exitCode=" + exitCode
                                + ", command=" + String.join(" ", command)
                                + ", output=" + output
                );
            }

            return output.toString();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to execute Git command: " + String.join(" ", command),
                    e
            );
        }
    }
}