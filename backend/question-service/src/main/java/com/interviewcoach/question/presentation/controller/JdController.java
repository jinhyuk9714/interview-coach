package com.interviewcoach.question.presentation.controller;

import com.interviewcoach.question.application.dto.request.CreateJdRequest;
import com.interviewcoach.question.application.dto.response.JdAnalysisResponse;
import com.interviewcoach.question.application.dto.response.JdResponse;
import com.interviewcoach.question.application.service.JdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "JD 관리", description = "채용공고 등록, 분석, 삭제")
@Validated
@RestController
@RequestMapping("/api/v1/jd")
@RequiredArgsConstructor
public class JdController {

    private final JdService jdService;

    @Operation(summary = "JD 등록", description = "새로운 채용공고(JD)를 등록")
    @ApiResponse(responseCode = "201", description = "JD 등록 성공")
    @PostMapping
    public ResponseEntity<JdResponse> createJd(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateJdRequest request) {
        JdResponse response = jdService.createJd(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "JD 목록 조회", description = "사용자의 JD 목록 조회 (Redis 캐싱)")
    @ApiResponse(responseCode = "200", description = "JD 목록 반환")
    @GetMapping
    public ResponseEntity<List<JdResponse>> getJdList(@RequestHeader("X-User-Id") Long userId) {
        List<JdResponse> response = jdService.getJdList(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "JD 상세 조회", description = "JD ID로 상세 정보 조회 (Redis 캐싱)")
    @ApiResponse(responseCode = "200", description = "JD 상세 정보 반환")
    @ApiResponse(responseCode = "404", description = "JD를 찾을 수 없음")
    @GetMapping("/{id}")
    public ResponseEntity<JdResponse> getJd(@PathVariable @Positive(message = "ID는 양수여야 합니다") Long id) {
        JdResponse response = jdService.getJd(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "JD 분석", description = "AI로 JD를 분석하여 필요 스킬 추출")
    @ApiResponse(responseCode = "200", description = "JD 분석 결과 반환")
    @ApiResponse(responseCode = "404", description = "JD를 찾을 수 없음")
    @PostMapping("/{id}/analyze")
    public ResponseEntity<JdAnalysisResponse> analyzeJd(@PathVariable @Positive(message = "ID는 양수여야 합니다") Long id) {
        JdAnalysisResponse response = jdService.analyzeJd(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "JD 삭제", description = "JD를 삭제")
    @ApiResponse(responseCode = "204", description = "JD 삭제 성공")
    @ApiResponse(responseCode = "404", description = "JD를 찾을 수 없음")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJd(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable @Positive(message = "ID는 양수여야 합니다") Long id) {
        jdService.deleteJd(userId, id);
        return ResponseEntity.noContent().build();
    }
}
