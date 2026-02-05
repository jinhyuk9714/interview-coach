package com.interviewcoach.question.presentation.controller;

import com.interviewcoach.question.application.dto.request.CreateJdRequest;
import com.interviewcoach.question.application.dto.response.JdAnalysisResponse;
import com.interviewcoach.question.application.dto.response.JdResponse;
import com.interviewcoach.question.application.service.JdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jd")
@RequiredArgsConstructor
public class JdController {

    private final JdService jdService;

    @PostMapping
    public ResponseEntity<JdResponse> createJd(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateJdRequest request) {
        JdResponse response = jdService.createJd(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<JdResponse>> getJdList(@RequestHeader("X-User-Id") Long userId) {
        List<JdResponse> response = jdService.getJdList(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JdResponse> getJd(@PathVariable Long id) {
        JdResponse response = jdService.getJd(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<JdAnalysisResponse> analyzeJd(@PathVariable Long id) {
        JdAnalysisResponse response = jdService.analyzeJd(id);
        return ResponseEntity.ok(response);
    }
}
