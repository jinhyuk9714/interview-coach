package com.interviewcoach.feedback.presentation.controller;

import com.interviewcoach.feedback.application.dto.request.RecordAnswerRequest;
import com.interviewcoach.feedback.application.dto.response.StatisticsResponse;
import com.interviewcoach.feedback.application.dto.response.UserStatisticsSummaryResponse;
import com.interviewcoach.feedback.application.service.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public ResponseEntity<UserStatisticsSummaryResponse> getStatistics(
            @RequestHeader("X-User-Id") Long userId) {
        UserStatisticsSummaryResponse response = statisticsService.getStatistics(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{category}")
    public ResponseEntity<StatisticsResponse> getStatisticsByCategory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String category) {
        StatisticsResponse response = statisticsService.getStatisticsByCategory(userId, category);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/record")
    public ResponseEntity<StatisticsResponse> recordAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RecordAnswerRequest request) {
        StatisticsResponse response = statisticsService.recordAnswer(userId, request);
        return ResponseEntity.ok(response);
    }
}
