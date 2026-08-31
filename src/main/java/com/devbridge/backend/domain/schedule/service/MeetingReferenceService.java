package com.devbridge.backend.domain.schedule.service;

import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.schedule.dto.MeetingReferenceRequest;
import com.devbridge.backend.domain.schedule.dto.MeetingReferenceResponse;
import com.devbridge.backend.domain.schedule.entity.Meeting;
import com.devbridge.backend.domain.schedule.entity.MeetingReference;
import com.devbridge.backend.domain.schedule.repository.MeetingParticipantRepository;
import com.devbridge.backend.domain.schedule.repository.MeetingReferenceRepository;
import com.devbridge.backend.domain.schedule.repository.MeetingRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.service.UserInternalService;
import com.devbridge.backend.global.common.exception.BusinessException;
import com.devbridge.backend.global.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingReferenceService {

    private final MeetingRepository meetingRepository;
    private final MeetingReferenceRepository meetingReferenceRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final UserInternalService userInternalService;

    private User resolveUser(String employeeId) {
        return userInternalService.findByEmployeeId(employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_USER_NOT_FOUND,
                        "해당 사용자가 존재하지 않습니다: " + employeeId));
    }

    @Transactional
    public MeetingReferenceResponse addReference(String meetingId, String employeeId, MeetingReferenceRequest request) {
        User user = resolveUser(employeeId);
        meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_PARTICIPANT));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_MEETING_NOT_FOUND));

        MeetingReference reference = createReference(meeting, employeeId, request);

        return MeetingReferenceResponse.from(reference);
    }

    @Transactional
    public void deleteReference(String meetingId, String referenceId, String employeeId) {
        User user = resolveUser(employeeId);
        meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_PARTICIPANT));

        MeetingReference reference = meetingReferenceRepository.findByIdAndMeetingId(referenceId, meetingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_REFERENCE_NOT_FOUND));

        meetingReferenceRepository.delete(reference);
    }

    @Transactional
    public MeetingReference createReference(Meeting meeting, String employeeId, MeetingReferenceRequest request) {
        KnowledgeDocument document = null;
        if (request.documentId() != null) {
            document = knowledgeDocumentRepository.findById(request.documentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_DOCUMENT_NOT_FOUND));
        }

        MeetingReference reference = MeetingReference.builder()
                .meeting(meeting)
                .employeeId(employeeId)
                .referenceType(request.referenceType())
                .document(document)
                .fileUrl(request.fileUrl())
                .title(request.title())
                .build();

        return meetingReferenceRepository.save(reference);
    }

    @Transactional(readOnly = true)
    public List<MeetingReferenceResponse> getReferences(String meetingId) {
        return meetingReferenceRepository.findByMeetingIdOrderByCreatedAtDesc(meetingId).stream()
                .map(MeetingReferenceResponse::from)
                .toList();
    }
}
