package com.interviewcoach.feedback.infrastructure.streaming;

import com.interviewcoach.feedback.application.dto.response.FeedbackResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private static final Long DEFAULT_TIMEOUT = 60_000L; // 60 seconds

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long sessionId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        emitter.onCompletion(() -> {
            log.debug("SSE completed for session: {}", sessionId);
            emitters.remove(sessionId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE timeout for session: {}", sessionId);
            emitters.remove(sessionId);
        });

        emitter.onError(e -> {
            log.error("SSE error for session {}: {}", sessionId, e.getMessage());
            emitters.remove(sessionId);
        });

        emitters.put(sessionId, emitter);
        log.debug("Created SSE emitter for session: {}", sessionId);

        return emitter;
    }

    public void sendFeedback(Long sessionId, FeedbackResponse feedback) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("feedback")
                        .data(feedback));
                log.debug("Sent feedback to session: {}", sessionId);
            } catch (IOException e) {
                log.error("Failed to send feedback to session {}: {}", sessionId, e.getMessage());
                emitters.remove(sessionId);
            }
        }
    }

    public void sendProgress(Long sessionId, String message, int progress) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(Map.of("message", message, "progress", progress)));
            } catch (IOException e) {
                log.error("Failed to send progress to session {}: {}", sessionId, e.getMessage());
                emitters.remove(sessionId);
            }
        }
    }

    public void complete(Long sessionId) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(Map.of("status", "completed")));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to complete SSE for session {}: {}", sessionId, e.getMessage());
            } finally {
                emitters.remove(sessionId);
            }
        }
    }

    public void completeWithError(Long sessionId, Throwable error) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            emitter.completeWithError(error);
            emitters.remove(sessionId);
        }
    }
}
