package com.interviewcoach.feedback.presentation.controller;

import com.interviewcoach.feedback.application.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "피드백", description = "AI 피드백 스트리밍 (SSE)")
@Validated
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "피드백 스트리밍 (GET)", description = "SSE를 통한 실시간 AI 피드백 + 꼬리 질문 스트리밍")
    @ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공")
    @GetMapping(value = "/session/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFeedback(
            @PathVariable @Positive(message = "세션 ID는 양수여야 합니다") Long sessionId,
            @RequestParam(required = false) Long qnaId,
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String answer,
            @RequestParam(required = false, defaultValue = "0") Integer followUpDepth) {
        return feedbackService.streamFeedback(sessionId, qnaId, question, answer, followUpDepth);
    }

    @Operation(summary = "피드백 스트리밍 (POST)", description = "긴 답변을 위한 POST 방식 SSE 피드백 스트리밍")
    @ApiResponse(responseCode = "200", description = "SSE 스트림 연결 성공")
    // POST endpoint for long answers (no URL length limit)
    @PostMapping(value = "/session/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamFeedbackPost(
            @PathVariable @Positive(message = "세션 ID는 양수여야 합니다") Long sessionId,
            @RequestBody FeedbackRequest request) {
        int depth = request.followUpDepth() != null ? request.followUpDepth() : 0;
        return feedbackService.streamFeedback(sessionId, request.qnaId(), request.question(), request.answer(), depth);
    }

    public record FeedbackRequest(Long qnaId, String question, String answer, Integer followUpDepth) {}
}
