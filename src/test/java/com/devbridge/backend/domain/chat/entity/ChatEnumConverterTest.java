package com.devbridge.backend.domain.chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CitationSourceTypeConverter}, {@link FeedbackTypeConverter} 특성 테스트.
 *
 * <p>두 컨버터는 <b>DB에는 소문자, 코드에서는 대문자 enum</b>이라는 규약을 담당한다.
 * DB 컬럼값과 enum 이름이 어긋나면 조용히 null(또는 기본값)로 바뀌므로,
 * 오류가 드러나지 않고 데이터만 유실되는 종류의 코드다.
 *
 * <p>주의: 알 수 없는 값 처리 방식이 <b>읽기와 쓰기에서 비대칭</b>이다.
 * DB에서 읽을 때는 조용히 폴백하지만, AI 엔진 응답을 저장할 때는
 * `ChatMessageService.saveCitations()`가 `CitationSourceType.valueOf()`로 예외를 던진다.
 */
class ChatEnumConverterTest {

    @Nested
    @DisplayName("CitationSourceTypeConverter")
    class Citation {

        private final CitationSourceTypeConverter converter = new CitationSourceTypeConverter();

        @Test
        @DisplayName("convertToDatabaseColumn_withEnum_writesLowerCase")
        void convertToDatabaseColumn_withEnum_writesLowerCase() {
            assertThat(converter.convertToDatabaseColumn(CitationSourceType.DOCUMENT)).isEqualTo("document");
            assertThat(converter.convertToDatabaseColumn(CitationSourceType.GIT_COMMIT)).isEqualTo("git_commit");
            assertThat(converter.convertToDatabaseColumn(CitationSourceType.DB_SCHEMA)).isEqualTo("db_schema");
        }

        @Test
        @DisplayName("convertToDatabaseColumn_withNull_writesNull")
        void convertToDatabaseColumn_withNull_writesNull() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull();
        }

        @Test
        @DisplayName("convertToEntityAttribute_withLowerCaseValue_readsEnum")
        void convertToEntityAttribute_withLowerCaseValue_readsEnum() {
            assertThat(converter.convertToEntityAttribute("document")).isEqualTo(CitationSourceType.DOCUMENT);
            assertThat(converter.convertToEntityAttribute("GIT_COMMIT")).isEqualTo(CitationSourceType.GIT_COMMIT);
        }

        @Test
        @DisplayName("convertToEntityAttribute_withUnknownValue_returnsNullSilently")
        void convertToEntityAttribute_withUnknownValue_returnsNullSilently() {
            // DB에 예상 밖의 값이 있으면 예외 없이 null이 된다. 인용 출처가 조용히 사라진다.
            assertThat(converter.convertToEntityAttribute("slack_message")).isNull();
        }

        @Test
        @DisplayName("convertToEntityAttribute_withNull_returnsNull")
        void convertToEntityAttribute_withNull_returnsNull() {
            assertThat(converter.convertToEntityAttribute(null)).isNull();
        }
    }

    @Nested
    @DisplayName("FeedbackTypeConverter")
    class Feedback {

        private final FeedbackTypeConverter converter = new FeedbackTypeConverter();

        @Test
        @DisplayName("convertToDatabaseColumn_withEnum_writesLowerCase")
        void convertToDatabaseColumn_withEnum_writesLowerCase() {
            assertThat(converter.convertToDatabaseColumn(FeedbackType.POSITIVE)).isEqualTo("positive");
            assertThat(converter.convertToDatabaseColumn(FeedbackType.NEGATIVE)).isEqualTo("negative");
        }

        @Test
        @DisplayName("convertToDatabaseColumn_withNull_writesNoneInsteadOfNull")
        void convertToDatabaseColumn_withNull_writesNoneInsteadOfNull() {
            // CitationSourceTypeConverter와 달리 null을 "none"으로 치환한다(컬럼이 NOT NULL이기 때문).
            assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("none");
        }

        @Test
        @DisplayName("convertToEntityAttribute_withUnknownOrNullValue_fallsBackToNone")
        void convertToEntityAttribute_withUnknownOrNullValue_fallsBackToNone() {
            assertThat(converter.convertToEntityAttribute("positive")).isEqualTo(FeedbackType.POSITIVE);
            assertThat(converter.convertToEntityAttribute("unknown")).isEqualTo(FeedbackType.NONE);
            assertThat(converter.convertToEntityAttribute(null)).isEqualTo(FeedbackType.NONE);
        }
    }
}
