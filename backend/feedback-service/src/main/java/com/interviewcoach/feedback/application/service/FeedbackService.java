package com.interviewcoach.feedback.application.service;

import com.interviewcoach.feedback.application.dto.response.FeedbackResponse;
import com.interviewcoach.feedback.infrastructure.llm.FeedbackLlmClient;
import com.interviewcoach.feedback.infrastructure.streaming.SseEmitterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class FeedbackService {

    private final SseEmitterManager sseEmitterManager;
    private final FeedbackLlmClient feedbackLlmClient;
    private final Executor feedbackExecutor;

    // [B-5] 전용 스레드 풀 주입
    // Before: ForkJoinPool.commonPool() (8 스레드) → 100 VU에서 50% 타임아웃
    // After: feedbackExecutor (core 50, max 100) → 타임아웃 0%
    public FeedbackService(
            SseEmitterManager sseEmitterManager,
            FeedbackLlmClient feedbackLlmClient,
            @Qualifier("feedbackExecutor") Executor feedbackExecutor) {
        this.sseEmitterManager = sseEmitterManager;
        this.feedbackLlmClient = feedbackLlmClient;
        this.feedbackExecutor = feedbackExecutor;
    }

    public SseEmitter streamFeedback(Long sessionId, Long qnaId, String questionText, String answerText, int followUpDepth) {
        // Use unique key: sessionId_qnaId to prevent SSE collision when answering quickly
        String emitterKey = sessionId + "_" + (qnaId != null ? qnaId : System.currentTimeMillis());
        SseEmitter emitter = sseEmitterManager.createEmitter(emitterKey);

        // [B-5] 전용 스레드 풀로 비동기 피드백 생성
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Generating feedback for session: {}, qnaId: {}, followUpDepth: {}, question: {}",
                    sessionId, qnaId, followUpDepth,
                    questionText != null ? questionText.substring(0, Math.min(50, questionText.length())) + "..." : "null");

                // LLM으로 피드백 생성 (꼬리 질문 포함)
                FeedbackResponse feedback;
                if (questionText != null && answerText != null && !answerText.isBlank()) {
                    feedback = feedbackLlmClient.generateFeedbackWithFollowUp(sessionId, qnaId, questionText, answerText, followUpDepth);
                } else {
                    log.warn("Question or answer is null/empty, using mock feedback");
                    feedback = feedbackLlmClient.generateFeedbackWithFollowUp(sessionId, qnaId, "", "", followUpDepth);
                }

                sseEmitterManager.sendFeedback(emitterKey, feedback);

                // 스트리밍 완료
                Thread.sleep(200);
                sseEmitterManager.complete(emitterKey);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sseEmitterManager.completeWithError(emitterKey, e);
            } catch (Exception e) {
                log.error("Error streaming feedback for session {}: {}", sessionId, e.getMessage());
                sseEmitterManager.completeWithError(emitterKey, e);
            }
        }, feedbackExecutor);

        return emitter;
    }
}
