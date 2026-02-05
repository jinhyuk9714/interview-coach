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
            @RequestParam(required = false) String answer) {
        return feedbackService.streamFeedback(sessionId, qnaId, question, answer);
    }

    // POST endpoint for long answers (no URL length limit)
    @PostMapping(value = "/session/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFeedbackPost(
            @PathVariable Long sessionId,
            @RequestBody FeedbackRequest request) {
        return feedbackService.streamFeedback(sessionId, request.qnaId(), request.question(), request.answer());
    }

    public record FeedbackRequest(Long qnaId, String question, String answer) {}
}
