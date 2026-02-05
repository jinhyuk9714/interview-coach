package com.interviewcoach.feedback.application.service;

import com.interviewcoach.feedback.application.dto.response.FeedbackResponse;
import com.interviewcoach.feedback.infrastructure.llm.FeedbackLlmClient;
import com.interviewcoach.feedback.infrastructure.streaming.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final SseEmitterManager sseEmitterManager;
    private final FeedbackLlmClient feedbackLlmClient;

    public SseEmitter streamFeedback(Long sessionId, Long qnaId, String questionText, String answerText) {
        // Use unique key: sessionId_qnaId to prevent SSE collision when answering quickly
        String emitterKey = sessionId + "_" + (qnaId != null ? qnaId : System.currentTimeMillis());
        SseEmitter emitter = sseEmitterManager.createEmitter(emitterKey);

        // 비동기로 피드백 생성 및 스트리밍
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Generating feedback for session: {}, qnaId: {}, question: {}", sessionId, qnaId,
                    questionText != null ? questionText.substring(0, Math.min(50, questionText.length())) + "..." : "null");

                // LLM으로 피드백 생성
                FeedbackResponse feedback;
                if (questionText != null && answerText != null && !answerText.isBlank()) {
                    feedback = feedbackLlmClient.generateFeedback(sessionId, qnaId, questionText, answerText);
                } else {
                    log.warn("Question or answer is null/empty, using mock feedback");
                    feedback = feedbackLlmClient.generateFeedback(sessionId, qnaId, "", "");
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
        });

        return emitter;
    }
}
