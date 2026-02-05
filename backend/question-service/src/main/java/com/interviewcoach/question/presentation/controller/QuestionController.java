package com.interviewcoach.question.presentation.controller;

import com.interviewcoach.question.application.dto.request.GenerateQuestionsRequest;
import com.interviewcoach.question.application.dto.response.GeneratedQuestionsResponse;
import com.interviewcoach.question.application.dto.response.QuestionResponse;
import com.interviewcoach.question.application.dto.response.SimilarQuestionsResponse;
import com.interviewcoach.question.application.service.QuestionGenerationService;
import com.interviewcoach.question.application.service.SimilarQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionGenerationService questionGenerationService;
    private final SimilarQuestionService similarQuestionService;

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

    /**
     * 쿼리 텍스트 기반 유사 질문 검색
     *
     * @param query 검색 쿼리 (JD 텍스트 또는 키워드)
     * @param questionType 질문 유형 필터 (optional: technical, behavioral, mixed)
     * @param skills 스킬 필터 (optional, comma-separated)
     * @param limit 최대 결과 수 (default: 5)
     * @return 유사 질문 목록
     */
    @GetMapping("/similar")
    public ResponseEntity<SimilarQuestionsResponse> searchSimilarQuestions(
            @RequestParam String query,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) List<String> skills,
            @RequestParam(defaultValue = "5") int limit) {

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
    @GetMapping("/jd/{jdId}/similar")
    public ResponseEntity<SimilarQuestionsResponse> searchSimilarQuestionsByJd(
            @PathVariable Long jdId,
            @RequestParam(defaultValue = "5") int limit) {

        SimilarQuestionsResponse response = similarQuestionService.searchByJdId(
                jdId, Math.min(limit, 10));
        return ResponseEntity.ok(response);
    }

    /**
     * RAG 서비스 상태 확인
     *
     * @return RAG 활성화 여부
     */
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
