package com.devbridge.backend.global.config;

import com.devbridge.backend.domain.datasource.entity.DataSource;
import com.devbridge.backend.domain.datasource.entity.GitCommit;
import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.DataSourceRepository;
import com.devbridge.backend.domain.datasource.repository.GitCommitRepository;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.workspace.entity.Workspace;
import com.devbridge.backend.domain.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardDataInitializer implements ApplicationRunner {

    private final WorkspaceRepository workspaceRepository;
    private final DataSourceRepository dataSourceRepository;
    private final GitCommitRepository gitCommitRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<Workspace> workspaces = workspaceRepository.findAll();
        if (workspaces.isEmpty()) return;

        for (Workspace ws : workspaces) {
            try {
                initWorkspaceDashboardData(ws);
            } catch (Exception e) {
                log.warn("Failed to init dashboard data for workspace {}: {}", ws.getId(), e.getMessage());
            }
        }
    }

    private void initWorkspaceDashboardData(Workspace ws) {
        // Skip if this workspace already has git commits
        long commitCount = gitCommitRepository.countByWorkspace_Id(ws.getId());
        if (commitCount > 0) return;

        // Find or create a DataSource for this workspace (GitCommit requires source_id FK)
        DataSource ds = getOrCreateDataSource(ws);
        if (ds == null) {
            log.warn("Skipping dashboard data for workspace {}: could not create DataSource", ws.getId());
            return;
        }

        log.info("Adding demo dashboard data for workspace: {}", ws.getName());

        // Save demo git commits
        LocalDateTime now = LocalDateTime.now();
        String[][] commits = {
            {"a1b2c3d1", "feat: implement dashboard API structure", "Kim Hyunsoo", "feature/dashboard"},
            {"b2c3d4e5", "fix: update task status flow and bug fix", "Lee Wonbin", "feature/task"},
            {"c3d4e5f6", "feat: add workspace member list component", "Choi Hyungsu", "feature/member"},
            {"d4e5f6a7", "chore: update project dependencies", "Kim Hyunsoo", "develop"},
        };
        for (int i = 0; i < commits.length; i++) {
            String hash = commits[i][0] + "e9f8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f";
            GitCommit gc = GitCommit.builder()
                    .workspace(ws).dataSource(ds)
                    .commitHash(hash).shortHash(commits[i][0])
                    .commitMessage(commits[i][1])
                    .authorName(commits[i][2]).branchName(commits[i][3])
                    .pushedAt(now.minusDays(commits.length - 1 - i))
                    .build();
            gitCommitRepository.save(gc);
        }

        // Save demo documents
        long docCount = knowledgeDocumentRepository.findByDataSource_Workspace_IdOrderByCreatedAtDesc(ws.getId()).size();
        if (docCount == 0) {
            String[][] docs = {
                {"API Specification.md", "MD"}, {"Database Schema.docx", "DOC"},
                {"Sprint Meeting Notes", "NOTE"}, {"Task Checklist", "LIST"},
            };
            for (int i = 0; i < docs.length; i++) {
                KnowledgeDocument doc = KnowledgeDocument.builder()
                        .workspace(ws).dataSource(ds)
                        .title(docs[i][0]).documentType(docs[i][1])
                        .filePath("/demo/" + (i + 1))
                        .fileSize(1024L * (i + 1))
                        .build();
                knowledgeDocumentRepository.save(doc);
            }
        }
    }

    private DataSource getOrCreateDataSource(Workspace ws) {
        List<DataSource> existing = dataSourceRepository.findByWorkspace_Id(ws.getId());
        if (!existing.isEmpty()) return existing.get(0);

        // Create a GIT + DOC data source so GitCommit (source_id NOT NULL) can save
        DataSource ds = DataSource.builder()
                .workspace(ws)
                .sourceType("GIT")
                .sourceName("Demo Repository")
                .status("CONNECTED")
                .build();
        return dataSourceRepository.save(ds);
    }
}
