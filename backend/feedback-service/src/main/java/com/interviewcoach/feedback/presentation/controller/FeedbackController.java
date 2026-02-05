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
            @RequestParam(required = false) Long qnaId) {
        return feedbackService.streamFeedback(sessionId, qnaId);
    }
}
