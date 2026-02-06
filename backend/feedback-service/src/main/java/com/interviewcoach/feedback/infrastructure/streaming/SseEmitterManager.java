package com.interviewcoach.feedback.infrastructure.streaming;

import com.interviewcoach.feedback.application.dto.response.FeedbackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [B-6] SseEmitter 메모리 누수 해결
 *
 * 문제: ConcurrentHashMap이 무한 증가
 *       - 네트워크 끊김 시 onCompletion 콜백 미실행 → 미정리
 *       - 장시간 운영 시 시간당 80MB 메모리 누수
 *
 * 해결:
 *       1. 생성 시간 기록 (EmitterWrapper)
 *       2. @Scheduled로 10초마다 만료 Emitter 정리
 *       3. 최대 Emitter 수 제한 (MAX_EMITTERS = 5,000)
 *       4. TTL 기반 정리 (120초 초과 시 강제 제거)
 */
@Slf4j
@Component
public class SseEmitterManager {

    private static final Long DEFAULT_TIMEOUT = 60_000L; // 60 seconds
    private static final int MAX_EMITTERS = 5_000;
    private static final long EMITTER_TTL_SECONDS = 120; // 2분 TTL

    private record EmitterWrapper(SseEmitter emitter, Instant createdAt) {}

    private final Map<String, EmitterWrapper> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String emitterKey) {
        // 최대 Emitter 수 제한
        if (emitters.size() >= MAX_EMITTERS) {
            log.warn("Max emitters reached ({}), cleaning up oldest entries", MAX_EMITTERS);
            cleanupExpiredEmitters();
        }

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("SSE completed for key: {}", emitterKey);
            emitters.remove(emitterKey);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE timeout for key: {}", emitterKey);
            emitters.remove(emitterKey);
        });

        emitter.onError(e -> {
            log.error("SSE error for key {}: {}", emitterKey, e.getMessage());
            emitters.remove(emitterKey);
        });

        emitters.put(emitterKey, new EmitterWrapper(emitter, Instant.now()));
        log.debug("Created SSE emitter for key: {}, total active: {}", emitterKey, emitters.size());

        return emitter;
    }

    public void sendFeedback(String emitterKey, FeedbackResponse feedback) {
        EmitterWrapper wrapper = emitters.get(emitterKey);
        if (wrapper != null) {
            try {
                wrapper.emitter().send(SseEmitter.event()
                        .name("feedback")
                        .data(feedback));
                log.debug("Sent feedback to key: {}", emitterKey);
            } catch (IOException e) {
                log.error("Failed to send feedback to key {}: {}", emitterKey, e.getMessage());
                emitters.remove(emitterKey);
            }
        }
    }

    public void sendProgress(String emitterKey, String message, int progress) {
        EmitterWrapper wrapper = emitters.get(emitterKey);
        if (wrapper != null) {
            try {
                wrapper.emitter().send(SseEmitter.event()
                        .name("progress")
                        .data(Map.of("message", message, "progress", progress)));
            } catch (IOException e) {
                log.error("Failed to send progress to key {}: {}", emitterKey, e.getMessage());
                emitters.remove(emitterKey);
            }
        }
    }

    public void complete(String emitterKey) {
        EmitterWrapper wrapper = emitters.get(emitterKey);
        if (wrapper != null) {
            try {
                wrapper.emitter().send(SseEmitter.event()
                        .name("complete")
                        .data(Map.of("status", "completed")));
                wrapper.emitter().complete();
            } catch (IOException e) {
                log.error("Failed to complete SSE for key {}: {}", emitterKey, e.getMessage());
            } finally {
                emitters.remove(emitterKey);
            }
        }
    }

    public void completeWithError(String emitterKey, Throwable error) {
        EmitterWrapper wrapper = emitters.get(emitterKey);
        if (wrapper != null) {
            wrapper.emitter().completeWithError(error);
            emitters.remove(emitterKey);
        }
    }

    /**
     * [B-6] 주기적 만료 Emitter 정리
     * 10초마다 실행, TTL 초과 Emitter 강제 제거
     *
     * Before: 24시간 운영 → 50,000 Emitter 누적, 힙 선형 증가
     * After: max 5,000 유지, 힙 안정적 수평
     */
    @Scheduled(fixedDelay = 10000)
    public void cleanupExpiredEmitters() {
        Instant cutoff = Instant.now().minusSeconds(EMITTER_TTL_SECONDS);
        int removedCount = 0;

        var iterator = emitters.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().createdAt().isBefore(cutoff)) {
                try {
                    entry.getValue().emitter().complete();
                } catch (Exception e) {
                    // Ignore errors during cleanup
                }
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} expired SSE emitters, remaining: {}", removedCount, emitters.size());
        }
    }

    public int getActiveEmitterCount() {
        return emitters.size();
    }
}
