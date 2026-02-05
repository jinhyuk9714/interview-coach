package com.interviewcoach.question.presentation.controller;

import com.interviewcoach.question.application.dto.request.GenerateQuestionsRequest;
import com.interviewcoach.question.application.dto.response.GeneratedQuestionsResponse;
import com.interviewcoach.question.application.dto.response.QuestionResponse;
import com.interviewcoach.question.application.service.QuestionGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGenerationService questionGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<GeneratedQuestionsResponse> generateQuestions(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody GenerateQuestionsRequest request) {
        GeneratedQuestionsResponse response = questionGenerationService.generateQuestions(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/jd/{jdId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByJd(@PathVariable Long jdId) {
        List<QuestionResponse> response = questionGenerationService.getQuestionsByJd(jdId);
        return ResponseEntity.ok(response);
    }
}
