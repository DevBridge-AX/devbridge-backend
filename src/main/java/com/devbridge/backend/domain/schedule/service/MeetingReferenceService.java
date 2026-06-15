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

    @Transactional
    public MeetingReferenceResponse addReference(String meetingId, String employeeId, MeetingReferenceRequest request) {
        meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의의 참석자가 아닙니다."));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의가 존재하지 않습니다."));

        MeetingReference reference = createReference(meeting, employeeId, request);

        return MeetingReferenceResponse.from(reference);
    }

    @Transactional
    public void deleteReference(String meetingId, String referenceId, String employeeId) {
        meetingParticipantRepository.findByMeetingIdAndEmployeeId(meetingId, employeeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의의 참석자가 아닙니다."));

        MeetingReference reference = meetingReferenceRepository.findByIdAndMeetingId(referenceId, meetingId)
                .orElseThrow(() -> new IllegalArgumentException("해당 회의의 첨부파일이 아닙니다."));

        meetingReferenceRepository.delete(reference);
    }

    @Transactional
    public MeetingReference createReference(Meeting meeting, String employeeId, MeetingReferenceRequest request) {
        KnowledgeDocument document = null;
        if (request.documentId() != null) {
            document = knowledgeDocumentRepository.findById(request.documentId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 문서가 존재하지 않습니다."));
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
