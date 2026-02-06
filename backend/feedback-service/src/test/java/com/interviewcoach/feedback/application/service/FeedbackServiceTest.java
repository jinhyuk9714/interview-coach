package com.interviewcoach.feedback.application.service;

import com.interviewcoach.feedback.application.dto.response.FeedbackResponse;
import com.interviewcoach.feedback.infrastructure.llm.FeedbackLlmClient;
import com.interviewcoach.feedback.infrastructure.streaming.SseEmitterManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService 단위 테스트")
class FeedbackServiceTest {

    @Mock
    private SseEmitterManager sseEmitterManager;

    @Mock
    private FeedbackLlmClient feedbackLlmClient;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        // Use direct executor so async tasks run synchronously in tests
        Executor directExecutor = Runnable::run;
        feedbackService = new FeedbackService(sseEmitterManager, feedbackLlmClient, directExecutor);
    }

    private static final Long SESSION_ID = 1L;
    private static final Long QNA_ID = 10L;

    @Nested
    @DisplayName("streamFeedback 메서드")
    class StreamFeedbackTest {

        @Test
        @DisplayName("피드백 스트리밍 시작 - SseEmitter 반환")
        void streamFeedback_ReturnsEmitter() {
            // given
            SseEmitter mockEmitter = new SseEmitter();
            String questionText = "Java의 GC에 대해 설명해주세요.";
            String answerText = "GC는 가비지 컬렉션으로...";
            String expectedKey = SESSION_ID + "_" + QNA_ID;

            given(sseEmitterManager.createEmitter(expectedKey)).willReturn(mockEmitter);

            // when
            SseEmitter result = feedbackService.streamFeedback(SESSION_ID, QNA_ID, questionText, answerText, 0);

            // then
            assertThat(result).isEqualTo(mockEmitter);
            verify(sseEmitterManager, times(1)).createEmitter(expectedKey);
        }

        @Test
        @DisplayName("피드백 스트리밍 - LLM 클라이언트 호출 확인")
        void streamFeedback_CallsLlmClient() throws InterruptedException {
            // given
            SseEmitter mockEmitter = new SseEmitter(30000L);
            String questionText = "Spring DI에 대해 설명해주세요.";
            String answerText = "DI는 의존성 주입으로...";
            String expectedKey = SESSION_ID + "_" + QNA_ID;

            FeedbackResponse mockFeedback = FeedbackResponse.builder()
                    .sessionId(SESSION_ID)
                    .qnaId(QNA_ID)
                    .score(80)
                    .strengths(List.of("핵심 개념을 잘 이해하고 있습니다"))
                    .improvements(List.of("더 구체적인 예시를 추가하세요"))
                    .tips(List.of("STAR 기법을 활용해보세요"))
                    .overallComment("전반적으로 좋은 답변입니다.")
                    .build();

            given(sseEmitterManager.createEmitter(expectedKey)).willReturn(mockEmitter);
            given(feedbackLlmClient.generateFeedbackWithFollowUp(SESSION_ID, QNA_ID, questionText, answerText, 0))
                    .willReturn(mockFeedback);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return null;
            }).when(sseEmitterManager).complete(expectedKey);

            // when
            feedbackService.streamFeedback(SESSION_ID, QNA_ID, questionText, answerText, 0);

            // then - 비동기 작업 완료 대기
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            verify(feedbackLlmClient, times(1)).generateFeedbackWithFollowUp(SESSION_ID, QNA_ID, questionText, answerText, 0);
            verify(sseEmitterManager, times(1)).sendFeedback(eq(expectedKey), eq(mockFeedback));
        }

        @Test
        @DisplayName("빈 답변으로 피드백 요청 - Mock 피드백 사용")
        void streamFeedback_EmptyAnswer_UsesMockFeedback() throws InterruptedException {
            // given
            SseEmitter mockEmitter = new SseEmitter(30000L);
            String questionText = "질문입니다";
            String answerText = "";
            String expectedKey = SESSION_ID + "_" + QNA_ID;

            FeedbackResponse mockFeedback = FeedbackResponse.builder()
                    .sessionId(SESSION_ID)
                    .qnaId(QNA_ID)
                    .score(75)
                    .strengths(List.of("기본 피드백"))
                    .improvements(List.of("더 노력하세요"))
                    .build();

            given(sseEmitterManager.createEmitter(expectedKey)).willReturn(mockEmitter);
            given(feedbackLlmClient.generateFeedbackWithFollowUp(SESSION_ID, QNA_ID, "", "", 0))
                    .willReturn(mockFeedback);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return null;
            }).when(sseEmitterManager).complete(expectedKey);

            // when
            feedbackService.streamFeedback(SESSION_ID, QNA_ID, questionText, answerText, 0);

            // then
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            verify(feedbackLlmClient).generateFeedbackWithFollowUp(SESSION_ID, QNA_ID, "", "", 0);
        }

        @Test
        @DisplayName("null 질문/답변으로 피드백 요청")
        void streamFeedback_NullInputs() throws InterruptedException {
            // given
            SseEmitter mockEmitter = new SseEmitter(30000L);
            String expectedKey = SESSION_ID + "_" + QNA_ID;

            FeedbackResponse mockFeedback = FeedbackResponse.builder()
                    .sessionId(SESSION_ID)
                    .qnaId(QNA_ID)
                    .score(75)
                    .build();

            given(sseEmitterManager.createEmitter(expectedKey)).willReturn(mockEmitter);
            given(feedbackLlmClient.generateFeedbackWithFollowUp(SESSION_ID, QNA_ID, "", "", 0))
                    .willReturn(mockFeedback);

            CountDownLatch latch = new CountDownLatch(1);
            doAnswer(invocation -> {
                latch.countDown();
                return null;
            }).when(sseEmitterManager).complete(expectedKey);

            // when
            feedbackService.streamFeedback(SESSION_ID, QNA_ID, null, null, 0);

            // then
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertThat(completed).isTrue();

            verify(feedbackLlmClient).generateFeedbackWithFollowUp(SESSION_ID, QNA_ID, "", "", 0);
        }
    }
}
