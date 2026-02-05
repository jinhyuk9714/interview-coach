package com.interviewcoach.interview.presentation.controller;

import com.interviewcoach.interview.application.dto.request.AddFollowUpRequest;
import com.interviewcoach.interview.application.dto.request.StartInterviewRequest;
import com.interviewcoach.interview.application.dto.request.SubmitAnswerRequest;
import com.interviewcoach.interview.application.dto.response.InterviewListResponse;
import com.interviewcoach.interview.application.dto.response.InterviewSessionResponse;
import com.interviewcoach.interview.application.dto.response.QnaResponse;
import com.interviewcoach.interview.application.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<InterviewSessionResponse> startInterview(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody StartInterviewRequest request) {
        InterviewSessionResponse response = interviewService.startInterview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<InterviewListResponse> getInterviews(@RequestHeader("X-User-Id") Long userId) {
        InterviewListResponse response = interviewService.getInterviews(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewSessionResponse> getInterview(@PathVariable Long id) {
        InterviewSessionResponse response = interviewService.getInterview(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/answer")
    public ResponseEntity<QnaResponse> submitAnswer(
            @PathVariable Long id,
            @Valid @RequestBody SubmitAnswerRequest request) {
        QnaResponse response = interviewService.submitAnswer(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<InterviewSessionResponse> completeInterview(@PathVariable Long id) {
        InterviewSessionResponse response = interviewService.completeInterview(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/qna/{questionOrder}/feedback")
    public ResponseEntity<QnaResponse> updateFeedback(
            @PathVariable Long id,
            @PathVariable Integer questionOrder,
            @RequestBody java.util.Map<String, Object> feedback) {
        QnaResponse response = interviewService.updateFeedback(id, questionOrder, feedback);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/follow-up")
    public ResponseEntity<QnaResponse> addFollowUpQuestion(
            @PathVariable Long id,
            @Valid @RequestBody AddFollowUpRequest request) {
        QnaResponse response = interviewService.addFollowUpQuestion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
