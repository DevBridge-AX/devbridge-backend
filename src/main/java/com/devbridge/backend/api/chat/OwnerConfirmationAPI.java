package com.devbridge.backend.api.chat;

import com.devbridge.backend.domain.chat.dto.OwnerConfirmationResponse;
import com.devbridge.backend.domain.chat.dto.SubmitOwnerAnswerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "OwnerConfirmation", description = "담당자 확인 요청 답변 API 명세")
@RequestMapping("/api/owner-confirmations")
public interface OwnerConfirmationAPI {

    @Operation(summary = "답변 제출", description = "배정된 담당자가 확인 요청에 답변을 제출합니다.")
    @PostMapping("/{confirmationId}/answer")
    ResponseEntity<OwnerConfirmationResponse> submitAnswer(
            @PathVariable("confirmationId") String confirmationId,
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody SubmitOwnerAnswerRequest request);

    @Operation(summary = "확인 요청 상세 조회", description = "특정 확인 요청의 상세 정보를 조회합니다.")
    @GetMapping("/{confirmationId}")
    ResponseEntity<OwnerConfirmationResponse> getOwnerConfirmationDetail(
            @PathVariable("confirmationId") String confirmationId,
            @AuthenticationPrincipal String employeeId);

    @Operation(summary = "내게 배정된 확인 요청 목록", description = "현재 사용자에게 배정된 확인 요청 목록을 조회합니다.")
    @GetMapping("/assigned")
    ResponseEntity<Page<OwnerConfirmationResponse>> getAssignedConfirmations(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String employeeId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);

    @Operation(summary = "내가 요청한 확인 목록", description = "현재 사용자가 요청한 확인 요청 및 답변 목록을 조회합니다.")
    @GetMapping("/requested")
    ResponseEntity<Page<OwnerConfirmationResponse>> getRequestedConfirmations(
            @RequestHeader("X-Workspace-Id") String workspaceId,
            @AuthenticationPrincipal String employeeId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size);
}
