package com.devbridge.backend.domain.workspace.service;

import com.devbridge.backend.domain.workspace.dto.AiSummaryResponse;
import com.devbridge.backend.global.config.fastapi.FastApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceAiSummaryService {

    private final WorkspaceDashboardService dashboardService;
    private final FastApiClient fastApiClient;

    public Mono<AiSummaryResponse> generateAiSummary(String workspaceId) {
        var summary = dashboardService.getSummary(workspaceId);
        var detail = dashboardService.getDetail(workspaceId);

        Map<String, Object> request = new HashMap<>();
        request.put("workspace_id", summary.getWorkspaceId());
        request.put("workspace_name", summary.getWorkspaceName());
        request.put("total_task_count", summary.getTotalTaskCount());
        request.put("assigned_task_count", summary.getAssignedTaskCount());
        request.put("in_progress_task_count", summary.getInProgressTaskCount());
        request.put("done_task_count", summary.getDoneTaskCount());
        request.put("delayed_task_count", summary.getDelayedTaskCount());
        request.put("progress_rate", summary.getProgressRate());
        request.put("member_count", summary.getMemberCount());
        request.put("recent_task_count", detail.getRecentTasks().size());
        request.put("recent_git_commit_count", detail.getRecentGitCommits().size());
        request.put("recent_document_count", detail.getRecentDocuments().size());

        return fastApiClient.getWorkspaceAiSummary(request)
                .map(response -> {
                    String aiSummary = (String) response.get("summary");
                    if (aiSummary == null || aiSummary.isBlank()) {
                        aiSummary = buildFallbackSummary(summary, detail);
                    }
                    String model = (String) response.getOrDefault("model", "fallback");
                    String mode = (String) response.getOrDefault("mode", "fallback");
                    return new AiSummaryResponse(aiSummary, model, mode);
                })
                .onErrorResume(e -> {
                    log.warn("AI Engine 호출 실패, fallback 요약 사용: {}", e.getMessage());
                    return Mono.just(new AiSummaryResponse(
                            buildFallbackSummary(summary, detail),
                            "fallback",
                            "fallback"
                    ));
                });
    }

    private String buildFallbackSummary(
            com.devbridge.backend.domain.workspace.dto.WorkspaceDashboardSummaryResponse summary,
            com.devbridge.backend.domain.workspace.dto.WorkspaceDashboardDetailResponse detail
    ) {
        var sb = new StringBuilder();
        sb.append(String.format(
                "📊 %s 워크스페이스에는 현재 총 %d개의 업무가 등록되어 있으며, 이 중 %d건이 완료(완료율 %d%%), %d건이 진행 중입니다.",
                summary.getWorkspaceName(), summary.getTotalTaskCount(),
                summary.getDoneTaskCount(), summary.getProgressRate(),
                summary.getInProgressTaskCount()
        ));

        if (summary.getDelayedTaskCount() > 0) {
            sb.append(String.format(" ⚠️ 지연 업무가 %d건 있어 우선 확인이 필요합니다.", summary.getDelayedTaskCount()));
        } else {
            sb.append(" ✅ 현재 지연 업무가 없어 일정 리스크가 낮습니다.");
        }

        int commitCount = detail.getRecentGitCommits().size();
        if (commitCount > 0) {
            sb.append(String.format(" 🔄 최근 %d건의 Git Commit이 있어 활발한 개발이 진행 중입니다.", commitCount));
        }

        int docCount = detail.getRecentDocuments().size();
        if (docCount > 0) {
            sb.append(String.format(" 📄 최근 %d건의 문서가 업데이트되었습니다.", docCount));
        }

        if (summary.getProgressRate() >= 70) {
            sb.append(" 💡 완료율이 높은 편이므로 마무리 작업과 산출물 검토에 집중하면 좋습니다.");
        } else if (summary.getProgressRate() >= 40) {
            sb.append(" 💡 진행 중인 업무의 병목 여부를 확인하고 우선순위를 조정하는 것이 좋습니다.");
        } else {
            sb.append(" 💡 초기 단계로 핵심 업무의 우선순위를 재정리하고 단기 목표를 설정하는 것이 좋습니다.");
        }

        return sb.toString();
    }
}