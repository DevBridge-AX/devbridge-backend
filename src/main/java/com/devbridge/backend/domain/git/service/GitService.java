package com.devbridge.backend.domain.git.service;

import com.devbridge.backend.domain.git.dto.GitCommitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GitService {

    @Value("${devbridge.git.repository-path:.}")
    private String repositoryPath;

    public List<GitCommitResponse> getRecentCommits(int limit) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);

        List<String> lines = executeGitLog(safeLimit);
        List<GitCommitResponse> commits = new ArrayList<>();

        for (String line : lines) {
            GitCommitResponse commit = parseGitLogLine(line);

            if (commit != null) {
                commits.add(commit);
            }
        }

        return commits;
    }

    private List<String> executeGitLog(int limit) {
        List<String> lines = new ArrayList<>();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "git",
                    "log",
                    "--pretty=format:%H%x1f%h%x1f%an%x1f%ae%x1f%ad%x1f%s",
                    "--date=format:%Y-%m-%d %H:%M:%S",
                    "-n",
                    String.valueOf(limit)
            );

            processBuilder.directory(new java.io.File(repositoryPath));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        lines.add(line);
                    }
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException("Failed to execute git log. exitCode=" + exitCode);
            }

            return lines;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read git commits.", e);
        }
    }

    private GitCommitResponse parseGitLogLine(String line) {
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
                .branchName(getCurrentBranchName())
                .build();
    }

    private String getCurrentBranchName() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "git",
                    "branch",
                    "--show-current"
            );

            processBuilder.directory(new java.io.File(repositoryPath));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String branchName = reader.readLine();
                int exitCode = process.waitFor();

                if (exitCode != 0 || branchName == null || branchName.isBlank()) {
                    return "unknown";
                }

                return branchName.trim();
            }
        } catch (Exception e) {
            return "unknown";
        }
    }
}