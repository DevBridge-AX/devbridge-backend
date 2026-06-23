package com.devbridge.backend.api.chat;

import com.devbridge.backend.domain.chat.dto.OwnerConfirmationResponse;
import com.devbridge.backend.domain.chat.dto.SubmitOwnerAnswerRequest;
import com.devbridge.backend.domain.chat.service.OwnerConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OwnerConfirmationController implements OwnerConfirmationAPI {

    private final OwnerConfirmationService ownerConfirmationService;

    @Override
    public ResponseEntity<OwnerConfirmationResponse> submitAnswer(String confirmationId, String employeeId,
                                                                  SubmitOwnerAnswerRequest request) {
        OwnerConfirmationResponse response = ownerConfirmationService.submitAnswer(
                confirmationId, employeeId, request.answerContent());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<OwnerConfirmationResponse> getOwnerConfirmationDetail(String confirmationId,
                                                                               String employeeId) {
        return ResponseEntity.ok(ownerConfirmationService.getDetail(confirmationId, employeeId));
    }

    @Override
    public ResponseEntity<Page<OwnerConfirmationResponse>> getAssignedConfirmations(String workspaceId,
                                                                                    String employeeId,
                                                                                    int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ownerConfirmationService.getAssignedConfirmations(employeeId, workspaceId, pageable));
    }

    @Override
    public ResponseEntity<Page<OwnerConfirmationResponse>> getRequestedConfirmations(String workspaceId,
                                                                                     String employeeId,
                                                                                     int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ownerConfirmationService.getRequestedConfirmations(employeeId, workspaceId, pageable));
    }
}
