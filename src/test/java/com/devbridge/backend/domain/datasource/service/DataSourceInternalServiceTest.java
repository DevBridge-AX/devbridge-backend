package com.devbridge.backend.domain.datasource.service;

import com.devbridge.backend.domain.datasource.entity.KnowledgeDocument;
import com.devbridge.backend.domain.datasource.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * {@link DataSourceInternalService} 테스트.
 *
 * <p>이 서비스는 도메인 간 {@code datasource} 조회의 창구다({@code DataSourceInternalService} javadoc 참고).
 * {@code findDocumentById}는 존재하지 않을 때 예외를 던지지 않고 {@link Optional#empty()}를 반환해야 한다 —
 * 호출부(schedule·chat)가 자기 도메인의 에러 코드로 예외를 던질 수 있어야 하기 때문이다.
 */
@ExtendWith(MockitoExtension.class)
class DataSourceInternalServiceTest {

    private static final String DOCUMENT_ID = "doc-1";

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    private DataSourceInternalService dataSourceInternalService;

    @BeforeEach
    void setUp() {
        dataSourceInternalService = new DataSourceInternalService(knowledgeDocumentRepository);
    }

    @Nested
    @DisplayName("findDocumentById")
    class FindDocumentById {

        @Test
        @DisplayName("findDocumentById_withExistingDocument_returnsDocument")
        void findDocumentById_withExistingDocument_returnsDocument() {
            KnowledgeDocument document = KnowledgeDocument.builder().id(DOCUMENT_ID).build();
            when(knowledgeDocumentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

            assertThat(dataSourceInternalService.findDocumentById(DOCUMENT_ID)).containsSame(document);
        }

        @Test
        @DisplayName("findDocumentById_withUnknownDocument_returnsEmpty")
        void findDocumentById_withUnknownDocument_returnsEmpty() {
            when(knowledgeDocumentRepository.findById("missing-doc")).thenReturn(Optional.empty());

            assertThat(dataSourceInternalService.findDocumentById("missing-doc")).isEmpty();
        }
    }
}
