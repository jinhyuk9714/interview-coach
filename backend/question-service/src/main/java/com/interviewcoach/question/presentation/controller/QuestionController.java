package com.interviewcoach.question.presentation.controller;

import com.interviewcoach.question.application.dto.request.GenerateQuestionsRequest;
import com.interviewcoach.question.application.dto.response.GeneratedQuestionsResponse;
import com.interviewcoach.question.application.dto.response.QuestionResponse;
import com.interviewcoach.question.application.dto.response.SimilarQuestionsResponse;
import com.interviewcoach.question.application.service.QuestionGenerationService;
import com.interviewcoach.question.application.service.SimilarQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "질문 생성", description = "면접 질문 생성 및 유사 질문 검색")
@Validated
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGenerationService questionGenerationService;
    private final SimilarQuestionService similarQuestionService;

    @Operation(summary = "면접 질문 생성", description = "JD 기반 AI 면접 질문 생성 (RAG, 취약 분야 우선 반영)")
    @ApiResponse(responseCode = "201", description = "질문 생성 성공")
    @PostMapping("/generate")
    public ResponseEntity<GeneratedQuestionsResponse> generateQuestions(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody GenerateQuestionsRequest request) {
        GeneratedQuestionsResponse response = questionGenerationService.generateQuestions(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "JD별 질문 목록 조회", description = "특정 JD에 대해 생성된 질문 목록 조회")
    @ApiResponse(responseCode = "200", description = "질문 목록 반환")
    @GetMapping("/jd/{jdId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByJd(@PathVariable @Positive(message = "JD ID는 양수여야 합니다") Long jdId) {
        List<QuestionResponse> response = questionGenerationService.getQuestionsByJd(jdId);
        return ResponseEntity.ok(response);
    }

    /**
     * 쿼리 텍스트 기반 유사 질문 검색
     *
     * @param query 검색 쿼리 (JD 텍스트 또는 키워드)
     * @param questionType 질문 유형 필터 (optional: technical, behavioral, mixed)
     * @param skills 스킬 필터 (optional, comma-separated)
     * @param limit 최대 결과 수 (default: 5)
     * @return 유사 질문 목록
     */
    @Operation(summary = "유사 질문 검색", description = "쿼리 텍스트 기반 유사 질문 검색 (RAG)")
    @ApiResponse(responseCode = "200", description = "유사 질문 목록 반환")
    @GetMapping("/similar")
    public ResponseEntity<SimilarQuestionsResponse> searchSimilarQuestions(
            @RequestParam @NotBlank(message = "검색 쿼리는 필수입니다") @Size(max = 200, message = "검색 쿼리는 200자 이내여야 합니다") String query,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {

        SimilarQuestionsResponse response = similarQuestionService.searchByQuery(
                query, questionType, skills, Math.min(limit, 10));
        return ResponseEntity.ok(response);
    }

    /**
     * JD ID 기반 유사 질문 검색
     * 해당 JD와 유사한 다른 JD의 질문들을 찾아 반환
     *
     * @param jdId JD ID
     * @param limit 최대 결과 수 (default: 5)
     * @return 유사 질문 목록
     */
    @Operation(summary = "JD 기반 유사 질문 검색", description = "JD ID 기반으로 유사한 다른 JD의 질문 검색")
    @ApiResponse(responseCode = "200", description = "유사 질문 목록 반환")
    @GetMapping("/jd/{jdId}/similar")
    public ResponseEntity<SimilarQuestionsResponse> searchSimilarQuestionsByJd(
            @PathVariable @Positive(message = "JD ID는 양수여야 합니다") Long jdId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(20) int limit) {

        SimilarQuestionsResponse response = similarQuestionService.searchByJdId(
                jdId, Math.min(limit, 10));
        return ResponseEntity.ok(response);
    }

    /**
     * RAG 서비스 상태 확인
     *
     * @return RAG 활성화 여부
     */
    @Operation(summary = "RAG 상태 확인", description = "RAG 서비스 활성화 여부 및 설정 정보 조회")
    @ApiResponse(responseCode = "200", description = "RAG 상태 정보 반환")
    @GetMapping("/rag/status")
    public ResponseEntity<Map<String, Object>> getRagStatus() {
        boolean enabled = similarQuestionService.isRagEnabled();
        return ResponseEntity.ok(Map.of(
                "ragEnabled", enabled,
                "embeddingModel", "AllMiniLmL6V2",
                "vectorStore", "ChromaDB"
        ));
    }
}
