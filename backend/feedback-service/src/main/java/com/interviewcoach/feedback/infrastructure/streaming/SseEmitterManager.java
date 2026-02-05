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

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String emitterKey) {
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

        emitters.put(emitterKey, emitter);
        log.debug("Created SSE emitter for key: {}", emitterKey);

        return emitter;
    }

    public void sendFeedback(String emitterKey, FeedbackResponse feedback) {
        SseEmitter emitter = emitters.get(emitterKey);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
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
        SseEmitter emitter = emitters.get(emitterKey);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(Map.of("message", message, "progress", progress)));
            } catch (IOException e) {
                log.error("Failed to send progress to key {}: {}", emitterKey, e.getMessage());
                emitters.remove(emitterKey);
            }
        }
    }

    public void complete(String emitterKey) {
        SseEmitter emitter = emitters.get(emitterKey);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(Map.of("status", "completed")));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to complete SSE for key {}: {}", emitterKey, e.getMessage());
            } finally {
                emitters.remove(emitterKey);
            }
        }
    }

    public void completeWithError(String emitterKey, Throwable error) {
        SseEmitter emitter = emitters.get(emitterKey);
        if (emitter != null) {
            emitter.completeWithError(error);
            emitters.remove(emitterKey);
        }
    }
}
