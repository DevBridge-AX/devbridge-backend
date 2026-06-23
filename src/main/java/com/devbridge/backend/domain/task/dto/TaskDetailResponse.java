package com.devbridge.backend.domain.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDetailResponse {

    private String id;

    private String workspaceId;
    private String workspaceName;

    private String requesterId;
    private String requesterName;
    private String requesterDepartment;

    private String assigneeId;
    private String assigneeName;
    private String assigneeDepartment;

    private String title;
    private String description;
    private String status;
    private String priority;
    private LocalDateTime dueDate;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String aiSummary;
    private String progressSummary;
    private String nextAction;
    private String riskLevel;

    private int documentCount;
    private int commitCount;
    private int deliverableCount;
    private int activityCount;

    private List<RelatedDocumentPreview> recentDocuments;
    private List<RelatedCommitPreview> recentCommits;
    private List<DeliverablePreview> recentDeliverables;
    private List<ActivityPreview> recentActivities;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedDocumentPreview {
        private String id;
        private String title;
        private String summary;
        private String keywords;
        private String riskLevel;
        private String nextAction;
        private String analysisStatus;
        private String analysisModel;
        private String analysisMode;
        private LocalDateTime analyzedAt;
        private LocalDateTime uploadedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RelatedCommitPreview {
        private String id;
        private String commitHash;
        private String shortHash;
        private String message;
        private String authorName;
        private String authorEmail;
        private String branchName;
        private String summary;
        private String impactArea;
        private String riskLevel;
        private String nextAction;
        private String indexStatus;
        private LocalDateTime committedAt;
        private LocalDateTime analyzedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeliverablePreview {
        private String id;
        private String title;
        private String fileUrl;
        private String submittedByName;
        private LocalDateTime submittedAt;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityPreview {
        private String id;
        private String type;
        private String message;
        private String actorName;
        private LocalDateTime createdAt;
    }
}