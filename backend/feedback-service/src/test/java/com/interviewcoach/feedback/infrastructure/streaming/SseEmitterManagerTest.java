package com.interviewcoach.feedback.infrastructure.streaming;

import com.interviewcoach.feedback.application.dto.response.FeedbackResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("SseEmitterManager 단위 테스트")
class SseEmitterManagerTest {

    private SseEmitterManager sseEmitterManager;

    @BeforeEach
    void setUp() {
        sseEmitterManager = new SseEmitterManager();
    }

    @Nested
    @DisplayName("createEmitter - Emitter 생성")
    class CreateEmitterTest {

        @Test
        @DisplayName("Emitter 생성 시 non-null 반환")
        void createEmitter_ReturnsNonNull() {
            // when
            SseEmitter emitter = sseEmitterManager.createEmitter("test-key");

            // then
            assertThat(emitter).isNotNull();
        }

        @Test
        @DisplayName("Emitter 생성 시 활성 카운트 증가")
        void createEmitter_IncreasesActiveCount() {
            // given
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(0);

            // when
            sseEmitterManager.createEmitter("key-1");
            sseEmitterManager.createEmitter("key-2");

            // then
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("같은 키로 Emitter 생성 시 기존 것을 교체")
        void createEmitter_ReplacesExistingKey() {
            // given
            SseEmitter first = sseEmitterManager.createEmitter("same-key");
            SseEmitter second = sseEmitterManager.createEmitter("same-key");

            // then
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(1);
            assertThat(second).isNotSameAs(first);
        }
    }

    @Nested
    @DisplayName("sendFeedback - 피드백 전송")
    class SendFeedbackTest {

        @Test
        @DisplayName("존재하는 Emitter에 피드백 전송 - 에러 없이 실행")
        void sendFeedback_ToExistingEmitter() {
            // given
            String key = "session_1_10";
            sseEmitterManager.createEmitter(key);

            FeedbackResponse feedback = FeedbackResponse.builder()
                    .sessionId(1L)
                    .qnaId(10L)
                    .score(85)
                    .strengths(List.of("좋은 답변입니다"))
                    .improvements(List.of("더 구체적인 예시가 필요합니다"))
                    .overallComment("전반적으로 양호합니다.")
                    .build();

            // when & then - IOException이 발생하지 않으면 성공
            assertThatCode(() -> sseEmitterManager.sendFeedback(key, feedback))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("존재하지 않는 키에 피드백 전송 - 무시됨")
        void sendFeedback_ToNonExistingEmitter() {
            // given
            FeedbackResponse feedback = FeedbackResponse.builder()
                    .sessionId(1L)
                    .qnaId(10L)
                    .score(80)
                    .build();

            // when & then - 에러 없이 무시됨
            assertThatCode(() -> sseEmitterManager.sendFeedback("non-existing-key", feedback))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("complete - Emitter 완료")
    class CompleteTest {

        @Test
        @DisplayName("Emitter 완료 후 제거됨")
        void complete_RemovesEmitter() {
            // given
            String key = "session_1_10";
            sseEmitterManager.createEmitter(key);
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(1);

            // when
            sseEmitterManager.complete(key);

            // then
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("존재하지 않는 키 완료 - 에러 없이 무시됨")
        void complete_NonExistingKey() {
            // when & then
            assertThatCode(() -> sseEmitterManager.complete("non-existing-key"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("completeWithError - 에러와 함께 Emitter 완료")
    class CompleteWithErrorTest {

        @Test
        @DisplayName("에러와 함께 Emitter 완료 후 제거됨")
        void completeWithError_RemovesEmitter() {
            // given
            String key = "session_1_10";
            sseEmitterManager.createEmitter(key);
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(1);

            // when
            sseEmitterManager.completeWithError(key, new RuntimeException("LLM API 오류"));

            // then
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("존재하지 않는 키에 에러 완료 - 무시됨")
        void completeWithError_NonExistingKey() {
            // when & then
            assertThatCode(() -> sseEmitterManager.completeWithError("non-existing-key", new RuntimeException("error")))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("cleanupExpiredEmitters - 만료 Emitter 정리")
    class CleanupExpiredEmittersTest {

        @Test
        @DisplayName("새로 생성된 Emitter는 정리되지 않음")
        void cleanupExpiredEmitters_DoesNotRemoveFreshEmitters() {
            // given
            sseEmitterManager.createEmitter("key-1");
            sseEmitterManager.createEmitter("key-2");
            sseEmitterManager.createEmitter("key-3");
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(3);

            // when - 방금 생성된 emitter는 TTL 이내이므로 정리되지 않음
            sseEmitterManager.cleanupExpiredEmitters();

            // then
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("Emitter가 없을 때 정리 호출 - 에러 없이 실행")
        void cleanupExpiredEmitters_EmptyMap() {
            // when & then
            assertThatCode(() -> sseEmitterManager.cleanupExpiredEmitters())
                    .doesNotThrowAnyException();
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getActiveEmitterCount - 활성 Emitter 수 조회")
    class GetActiveEmitterCountTest {

        @Test
        @DisplayName("초기 상태에서 0 반환")
        void getActiveEmitterCount_InitiallyZero() {
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("생성과 완료 후 정확한 카운트 반환")
        void getActiveEmitterCount_AfterCreateAndComplete() {
            // given
            sseEmitterManager.createEmitter("key-1");
            sseEmitterManager.createEmitter("key-2");
            sseEmitterManager.createEmitter("key-3");

            // when
            sseEmitterManager.complete("key-2");

            // then
            assertThat(sseEmitterManager.getActiveEmitterCount()).isEqualTo(2);
        }
    }
}
