package com.interviewcoach.feedback.application.service;

import com.interviewcoach.feedback.application.dto.response.FeedbackResponse;
import com.interviewcoach.feedback.infrastructure.streaming.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final SseEmitterManager sseEmitterManager;

    public SseEmitter streamFeedback(Long sessionId, Long qnaId) {
        SseEmitter emitter = sseEmitterManager.createEmitter(sessionId);

        // 비동기로 피드백 생성 및 스트리밍
        CompletableFuture.runAsync(() -> {
            try {
                // 피드백 생성 시뮬레이션 (실제로는 LLM 호출)
                Thread.sleep(500);

                FeedbackResponse feedback = generateMockFeedback(sessionId, qnaId);
                sseEmitterManager.sendFeedback(sessionId, feedback);

                // 스트리밍 완료
                Thread.sleep(200);
                sseEmitterManager.complete(sessionId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                sseEmitterManager.completeWithError(sessionId, e);
            } catch (Exception e) {
                log.error("Error streaming feedback for session {}: {}", sessionId, e.getMessage());
                sseEmitterManager.completeWithError(sessionId, e);
            }
        });

        return emitter;
    }

    private FeedbackResponse generateMockFeedback(Long sessionId, Long qnaId) {
        return FeedbackResponse.builder()
                .sessionId(sessionId)
                .qnaId(qnaId)
                .score(75)
                .strengths(List.of(
                        "핵심 개념을 정확하게 이해하고 있습니다",
                        "실제 경험을 바탕으로 설명했습니다"
                ))
                .improvements(List.of(
                        "더 구체적인 예시를 들어 설명하면 좋겠습니다",
                        "기술적 깊이를 더 보여주세요"
                ))
                .tips("답변 시 STAR 기법(상황-과제-행동-결과)을 활용하면 더 체계적인 답변이 됩니다.")
                .overallComment("전반적으로 좋은 답변이지만, 구체적인 수치와 결과를 포함하면 더 설득력이 있을 것입니다.")
                .build();
    }
}
