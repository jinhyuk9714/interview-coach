package com.interviewcoach.feedback.presentation.controller;

import com.interviewcoach.feedback.application.dto.request.RecordAnswerRequest;
import com.interviewcoach.feedback.application.dto.response.StatisticsResponse;
import com.interviewcoach.feedback.application.dto.response.UserStatisticsSummaryResponse;
import com.interviewcoach.feedback.application.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "통계", description = "학습 통계 및 일일 활동 기록")
@Validated
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "전체 통계 조회", description = "사용자의 카테고리별 전체 학습 통계 조회")
    @ApiResponse(responseCode = "200", description = "통계 정보 반환")
    @GetMapping
    public ResponseEntity<UserStatisticsSummaryResponse> getStatistics(
            @RequestHeader("X-User-Id") Long userId) {
        UserStatisticsSummaryResponse response = statisticsService.getStatistics(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "카테고리별 통계 조회", description = "특정 스킬 카테고리의 학습 통계 조회")
    @ApiResponse(responseCode = "200", description = "카테고리 통계 반환")
    @ApiResponse(responseCode = "404", description = "카테고리를 찾을 수 없음")
    @GetMapping("/{category}")
    public ResponseEntity<StatisticsResponse> getStatisticsByCategory(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable @NotBlank(message = "카테고리는 필수입니다") String category) {
        StatisticsResponse response = statisticsService.getStatisticsByCategory(userId, category);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "답변 통계 기록", description = "답변 결과를 통계에 기록 (비관적 락)")
    @ApiResponse(responseCode = "200", description = "통계 기록 성공")
    @PostMapping("/record")
    public ResponseEntity<StatisticsResponse> recordAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RecordAnswerRequest request) {
        StatisticsResponse response = statisticsService.recordAnswer(userId, request);
        return ResponseEntity.ok(response);
    }
}
