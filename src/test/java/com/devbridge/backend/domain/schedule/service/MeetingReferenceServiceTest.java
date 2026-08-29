package com.devbridge.backend.domain.schedule.service;

import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import com.devbridge.backend.domain.schedule.dto.MeetingReferenceRequest;
import com.devbridge.backend.domain.schedule.dto.MeetingReferenceResponse;
import com.devbridge.backend.domain.schedule.entity.Meeting;
import com.devbridge.backend.domain.schedule.entity.MeetingParticipant;
import com.devbridge.backend.domain.schedule.entity.MeetingReference;
import com.devbridge.backend.domain.schedule.entity.ReferenceType;
import com.devbridge.backend.domain.schedule.repository.MeetingParticipantRepository;
import com.devbridge.backend.domain.schedule.repository.MeetingReferenceRepository;
import com.devbridge.backend.domain.schedule.repository.MeetingRepository;
import com.devbridge.backend.domain.user.entity.User;
import com.devbridge.backend.domain.user.service.UserInternalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MeetingReferenceService} 특성 테스트(characterization test).
 *
 * <p>"올바른 동작"이 아니라 <b>현재 동작</b>을 그대로 고정하는 것이 목적이다.
 * 이 서비스는 {@code KnowledgeDocumentRepository}(C-4 대상)를 여전히 직접 참조하므로,
 * 그 참조 제거 작업 전에 예외 타입·메시지를 고정하는 안전망이 필요하다.
 * {@code UserRepository} 직접 참조는 {@link UserInternalService}로 치환 완료했다(C-3, refactor/schedule).
 */
@ExtendWith(MockitoExtension.class)
class MeetingReferenceServiceTest {

    private static final String MEETING_ID = "meeting-1";
    private static final String EMPLOYEE_ID = "EMP001";

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private MeetingReferenceRepository meetingReferenceRepository;

    @Mock
    private MeetingParticipantRepository meetingParticipantRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private UserInternalService userInternalService;

    private MeetingReferenceService meetingReferenceService;

    @BeforeEach
    void setUp() {
        meetingReferenceService = new MeetingReferenceService(
                meetingRepository,
                meetingReferenceRepository,
                meetingParticipantRepository,
                knowledgeDocumentRepository,
                userInternalService
        );
    }

    private User user() {
        return User.builder().id("user-1").employeeId(EMPLOYEE_ID).build();
    }

    private Meeting meeting() {
        return Meeting.builder().id(MEETING_ID).build();
    }

    private MeetingParticipant participant() {
        return MeetingParticipant.builder().id("participant-1").employeeId(EMPLOYEE_ID).build();
    }

    private MeetingReferenceRequest request(String documentId) {
        return new MeetingReferenceRequest(ReferenceType.DOC_LINK, documentId, null, "회의록");
    }

    private void givenUser() {
        when(userInternalService.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(user()));
    }

    private void givenParticipant() {
        when(meetingParticipantRepository.findByMeetingIdAndEmployeeId(MEETING_ID, EMPLOYEE_ID))
                .thenReturn(Optional.of(participant()));
    }

    private void stubSaveReturnsArgument() {
        when(meetingReferenceRepository.save(any(MeetingReference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("addReference")
    class AddReference {

        @Test
        @DisplayName("addReference_withoutDocumentId_savesReferenceWithNullDocument")
        void addReference_withoutDocumentId_savesReferenceWithNullDocument() {
            givenUser();
            givenParticipant();
            when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting()));
            stubSaveReturnsArgument();

            MeetingReferenceResponse response = meetingReferenceService.addReference(
                    MEETING_ID, EMPLOYEE_ID, request(null));

            assertThat(response.documentId()).isNull();
            assertThat(response.title()).isEqualTo("회의록");
            assertThat(response.referenceType()).isEqualTo(ReferenceType.DOC_LINK);

            ArgumentCaptor<MeetingReference> captor = ArgumentCaptor.forClass(MeetingReference.class);
            verify(meetingReferenceRepository).save(captor.capture());
            assertThat(captor.getValue().getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("addReference_withDocumentId_savesReferenceWithLinkedDocument")
        void addReference_withDocumentId_savesReferenceWithLinkedDocument() {
            givenUser();
            givenParticipant();
            when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting()));
            when(knowledgeDocumentRepository.findById("doc-1"))
                    .thenReturn(Optional.of(KnowledgeDocument.builder().id("doc-1").build()));
            stubSaveReturnsArgument();

            MeetingReferenceResponse response = meetingReferenceService.addReference(
                    MEETING_ID, EMPLOYEE_ID, request("doc-1"));

            assertThat(response.documentId()).isEqualTo("doc-1");
        }

        @Test
        @DisplayName("addReference_withUnknownEmployee_throwsIllegalArgumentException")
        void addReference_withUnknownEmployee_throwsIllegalArgumentException() {
            when(userInternalService.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> meetingReferenceService.addReference(MEETING_ID, EMPLOYEE_ID, request(null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 사용자가 존재하지 않습니다: " + EMPLOYEE_ID);

            verify(meetingReferenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("addReference_withNonParticipant_throwsIllegalArgumentException")
        void addReference_withNonParticipant_throwsIllegalArgumentException() {
            givenUser();
            when(meetingParticipantRepository.findByMeetingIdAndEmployeeId(MEETING_ID, EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> meetingReferenceService.addReference(MEETING_ID, EMPLOYEE_ID, request(null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 회의의 참석자가 아닙니다.");
        }

        @Test
        @DisplayName("addReference_withUnknownMeeting_throwsIllegalArgumentException")
        void addReference_withUnknownMeeting_throwsIllegalArgumentException() {
            givenUser();
            givenParticipant();
            when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> meetingReferenceService.addReference(MEETING_ID, EMPLOYEE_ID, request(null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 회의가 존재하지 않습니다.");
        }

        @Test
        @DisplayName("addReference_withUnknownDocumentId_throwsIllegalArgumentException")
        void addReference_withUnknownDocumentId_throwsIllegalArgumentException() {
            givenUser();
            givenParticipant();
            when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting()));
            when(knowledgeDocumentRepository.findById("missing-doc")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> meetingReferenceService.addReference(
                    MEETING_ID, EMPLOYEE_ID, request("missing-doc")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 문서가 존재하지 않습니다.");
        }
    }

    @Nested
    @DisplayName("deleteReference")
    class DeleteReference {

        private static final String REFERENCE_ID = "reference-1";

        @Test
        @DisplayName("deleteReference_withValidReference_deletesReference")
        void deleteReference_withValidReference_deletesReference() {
            givenUser();
            givenParticipant();
            MeetingReference reference = MeetingReference.builder().id(REFERENCE_ID).build();
            when(meetingReferenceRepository.findByIdAndMeetingId(REFERENCE_ID, MEETING_ID))
                    .thenReturn(Optional.of(reference));

            meetingReferenceService.deleteReference(MEETING_ID, REFERENCE_ID, EMPLOYEE_ID);

            verify(meetingReferenceRepository).delete(reference);
        }

        @Test
        @DisplayName("deleteReference_withUnknownEmployee_throwsIllegalArgumentException")
        void deleteReference_withUnknownEmployee_throwsIllegalArgumentException() {
            when(userInternalService.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> meetingReferenceService.deleteReference(MEETING_ID, REFERENCE_ID, EMPLOYEE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 사용자가 존재하지 않습니다: " + EMPLOYEE_ID);

            verify(meetingReferenceRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteReference_withNonParticipant_throwsIllegalArgumentException")
        void deleteReference_withNonParticipant_throwsIllegalArgumentException() {
            givenUser();
            when(meetingParticipantRepository.findByMeetingIdAndEmployeeId(MEETING_ID, EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> meetingReferenceService.deleteReference(MEETING_ID, REFERENCE_ID, EMPLOYEE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 회의의 참석자가 아닙니다.");
        }

        @Test
        @DisplayName("deleteReference_withUnknownReference_throwsIllegalArgumentException")
        void deleteReference_withUnknownReference_throwsIllegalArgumentException() {
            givenUser();
            givenParticipant();
            when(meetingReferenceRepository.findByIdAndMeetingId(REFERENCE_ID, MEETING_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> meetingReferenceService.deleteReference(MEETING_ID, REFERENCE_ID, EMPLOYEE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 회의의 첨부파일이 아닙니다.");
        }
    }

    @Nested
    @DisplayName("getReferences")
    class GetReferences {

        @Test
        @DisplayName("getReferences_withExistingReferences_returnsResponsesInRepositoryOrder")
        void getReferences_withExistingReferences_returnsResponsesInRepositoryOrder() {
            MeetingReference reference = MeetingReference.builder().id("reference-1").title("회의록").build();
            when(meetingReferenceRepository.findByMeetingIdOrderByCreatedAtDesc(MEETING_ID))
                    .thenReturn(List.of(reference));

            List<MeetingReferenceResponse> responses = meetingReferenceService.getReferences(MEETING_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).id()).isEqualTo("reference-1");
        }

        @Test
        @DisplayName("getReferences_withNoReferences_returnsEmptyList")
        void getReferences_withNoReferences_returnsEmptyList() {
            when(meetingReferenceRepository.findByMeetingIdOrderByCreatedAtDesc(MEETING_ID))
                    .thenReturn(List.of());

            assertThat(meetingReferenceService.getReferences(MEETING_ID)).isEmpty();
        }
    }
}
