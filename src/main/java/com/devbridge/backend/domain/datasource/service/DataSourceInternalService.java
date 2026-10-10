package com.devbridge.backend.domain.datasource.service;

import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 타 도메인이 {@code datasource} 도메인 데이터를 조회할 때 거치는 창구.
 * {@link KnowledgeDocumentRepository}를 직접 주입받던 타 도메인 서비스는 이 클래스를 통해 접근한다.
 *
 * <p>{@code UserInternalService}와 같은 규칙을 따른다. <b>존재하지 않을 때 예외를 던지지 않고
 * {@link Optional}을 반환한다.</b> 호출부마다 "문서 없음"에 대해 서로 다른 에러 코드·메시지를
 * 사용하며(예: {@code MeetingReferenceService}는 {@code SCHEDULE_DOCUMENT_NOT_FOUND},
 * {@code OwnerConfirmationService}는 {@code CHAT_DOCUMENT_NOT_FOUND}), 이는 특성 테스트로 고정되어 있다.
 *
 * <p><b>{@link KnowledgeDocument} 엔티티를 그대로 반환한다.</b> 호출부가 조회 결과를
 * {@code @ManyToOne} 연관관계(예: {@code MeetingReference.document}, {@code OwnerConfirmation.document})에
 * 그대로 대입해야 하기 때문이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataSourceInternalService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public Optional<KnowledgeDocument> findDocumentById(String documentId) {
        return knowledgeDocumentRepository.findById(documentId);
    }
}
