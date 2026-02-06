package com.interviewcoach.interview.presentation.controller;

import com.interviewcoach.interview.application.dto.request.AddFollowUpRequest;
import com.interviewcoach.interview.application.dto.request.StartInterviewRequest;
import com.interviewcoach.interview.application.dto.request.SubmitAnswerRequest;
import com.interviewcoach.interview.application.dto.response.InterviewListResponse;
import com.interviewcoach.interview.application.dto.response.InterviewSessionResponse;
import com.interviewcoach.interview.application.dto.response.QnaResponse;
import com.interviewcoach.interview.application.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "면접", description = "면접 세션 관리, 답변 제출, 꼬리질문")
@Validated
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "면접 세션 시작", description = "새로운 면접 세션을 생성하고 시작")
    @ApiResponse(responseCode = "201", description = "면접 세션 생성 성공")
    @PostMapping
    public ResponseEntity<InterviewSessionResponse> startInterview(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody StartInterviewRequest request) {
        InterviewSessionResponse response = interviewService.startInterview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "면접 기록 목록 조회", description = "사용자의 면접 기록 목록 조회 (Fetch Join)")
    @ApiResponse(responseCode = "200", description = "면접 기록 목록 반환")
    @GetMapping
    public ResponseEntity<InterviewListResponse> getInterviews(@RequestHeader("X-User-Id") Long userId) {
        InterviewListResponse response = interviewService.getInterviews(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "면접 기록 검색", description = "키워드로 면접 기록 검색 (질문/답변 전문 검색)")
    @ApiResponse(responseCode = "200", description = "검색 결과 반환")
    // [A-1] 면접 기록 검색
    @GetMapping("/search")
    public ResponseEntity<InterviewListResponse> searchInterviews(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @NotBlank(message = "검색 키워드는 필수입니다") @Size(max = 100, message = "검색 키워드는 100자 이내여야 합니다") String keyword) {
        InterviewListResponse response = interviewService.searchInterviews(userId, keyword);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "면접 상세 조회", description = "면접 세션 ID로 상세 정보 조회 (Fetch Join)")
    @ApiResponse(responseCode = "200", description = "면접 상세 정보 반환")
    @ApiResponse(responseCode = "404", description = "면접 세션을 찾을 수 없음")
    @GetMapping("/{id}")
    public ResponseEntity<InterviewSessionResponse> getInterview(@PathVariable @Positive(message = "ID는 양수여야 합니다") Long id) {
        InterviewSessionResponse response = interviewService.getInterview(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "답변 제출", description = "면접 질문에 대한 답변 제출")
    @ApiResponse(responseCode = "200", description = "답변 제출 성공")
    @ApiResponse(responseCode = "400", description = "일시정지 상태에서 답변 제출 불가")
    @PostMapping("/{id}/answer")
    public ResponseEntity<QnaResponse> submitAnswer(
            @PathVariable @Positive(message = "ID는 양수여야 합니다") Long id,
            @Valid @RequestBody SubmitAnswerRequest request) {
        QnaResponse response = interviewService.submitAnswer(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "면접 완료", description = "면접 세션을 완료 처리")
    @ApiResponse(responseCode = "200", description = "면접 완료 성공")
    @PostMapping("/{id}/complete")
    public ResponseEntity<InterviewSessionResponse> completeInterview(@PathVariable @Positive(message = "ID는 양수여야 합니다") Long id) {
        InterviewSessionResponse response = interviewService.completeInterview(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "면접 일시정지", description = "진행 중인 면접을 일시정지 상태로 변경")
    @ApiResponse(responseCode = "200", description = "면접 일시정지 성공")
    // [A-2] 면접 일시정지
    @PatchMapping("/{id}/pause")
    public ResponseEntity<InterviewSessionResponse> pauseInterview(@PathVariable @Positive(message = "ID는 양수여야 합니다") Long id) {
        InterviewSessionResponse response = interviewService.pauseInterview(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "면접 재개", description = "일시정지된 면접을 재개")
    @ApiResponse(responseCode = "200", description = "면접 재개 성공")
    // [A-2] 면접 재개
    @PatchMapping("/{id}/resume")
    public ResponseEntity<InterviewSessionResponse> resumeInterview(@PathVariable @Positive(message = "ID는 양수여야 합니다") Long id) {
        InterviewSessionResponse response = interviewService.resumeInterview(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "피드백 업데이트", description = "특정 Q&A의 피드백 정보 업데이트")
    @ApiResponse(responseCode = "200", description = "피드백 업데이트 성공")
    @PutMapping("/{id}/qna/{questionOrder}/feedback")
    public ResponseEntity<QnaResponse> updateFeedback(
            @PathVariable Long id,
            @PathVariable Integer questionOrder,
            @RequestBody java.util.Map<String, Object> feedback) {
        QnaResponse response = interviewService.updateFeedback(id, questionOrder, feedback);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "꼬리 질문 추가", description = "답변 점수 기반 꼬리 질문 추가 (점수 < 85, 깊이 < 2)")
    @ApiResponse(responseCode = "201", description = "꼬리 질문 추가 성공")
    @PostMapping("/{id}/follow-up")
    public ResponseEntity<QnaResponse> addFollowUpQuestion(
            @PathVariable @Positive(message = "ID는 양수여야 합니다") Long id,
            @Valid @RequestBody AddFollowUpRequest request) {
        QnaResponse response = interviewService.addFollowUpQuestion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
