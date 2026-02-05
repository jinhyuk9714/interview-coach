package com.interviewcoach.feedback.presentation.controller;

import com.interviewcoach.feedback.application.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping(value = "/session/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFeedback(
            @PathVariable Long sessionId,
            @RequestParam(required = false) Long qnaId,
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String answer,
            @RequestParam(required = false, defaultValue = "0") Integer followUpDepth) {
        return feedbackService.streamFeedback(sessionId, qnaId, question, answer, followUpDepth);
    }

    // POST endpoint for long answers (no URL length limit)
    @PostMapping(value = "/session/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFeedbackPost(
            @PathVariable Long sessionId,
            @RequestBody FeedbackRequest request) {
        int depth = request.followUpDepth() != null ? request.followUpDepth() : 0;
        return feedbackService.streamFeedback(sessionId, request.qnaId(), request.question(), request.answer(), depth);
    }

    public record FeedbackRequest(Long qnaId, String question, String answer, Integer followUpDepth) {}
}
