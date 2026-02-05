package com.interviewcoach.question.application.service;

import com.interviewcoach.question.application.dto.response.SimilarQuestionDto;
import com.interviewcoach.question.application.dto.response.SimilarQuestionsResponse;
import com.interviewcoach.question.infrastructure.rag.QuestionEmbeddingService;
import com.interviewcoach.question.infrastructure.rag.SimilarQuestionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 유사 질문 검색 서비스
 * ChromaDB 기반 벡터 검색을 통해 관련 질문을 찾아 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarQuestionService {

    private final QuestionEmbeddingService embeddingService;

    /**
     * 쿼리 텍스트 기반 유사 질문 검색
     *
     * @param query 검색 쿼리 (자연어)
     * @param questionType 질문 유형 필터 (null 허용)
     * @param skills 스킬 필터 (null 허용)
     * @param limit 최대 결과 수
     * @return 유사 질문 응답
     */
    public SimilarQuestionsResponse searchByQuery(
            String query,
            String questionType,
            List<String> skills,
            int limit) {

        if (!embeddingService.isAvailable()) {
            log.warn("RAG service not available, returning empty results");
            return SimilarQuestionsResponse.builder()
                    .query(query)
                    .totalCount(0)
                    .questions(List.of())
                    .ragEnabled(false)
                    .build();
        }

        log.info("Searching similar questions: query='{}', type={}, skills={}, limit={}",
                truncate(query, 50), questionType, skills, limit);

        List<SimilarQuestionResult> results = embeddingService.findSimilarQuestions(
                query, questionType, skills, limit);

        List<SimilarQuestionDto> questionDtos = results.stream()
                .map(this::toDto)
                .toList();

        log.info("Found {} similar questions for query", questionDtos.size());

        return SimilarQuestionsResponse.builder()
                .query(query)
                .totalCount(questionDtos.size())
                .questions(questionDtos)
                .ragEnabled(true)
                .build();
    }

    /**
     * JD ID 기반 유사 질문 검색
     * 해당 JD와 유사한 다른 JD의 질문들을 찾아 반환
     *
     * @param jdId JD ID
     * @param limit 최대 결과 수
     * @return 유사 질문 응답
     */
    public SimilarQuestionsResponse searchByJdId(Long jdId, int limit) {
        if (!embeddingService.isAvailable()) {
            log.warn("RAG service not available, returning empty results");
            return SimilarQuestionsResponse.builder()
                    .jdId(jdId)
                    .totalCount(0)
                    .questions(List.of())
                    .ragEnabled(false)
                    .build();
        }

        log.info("Searching similar questions for JD ID: {}", jdId);

        List<SimilarQuestionResult> results = embeddingService.findSimilarQuestionsByJdId(jdId, limit);

        List<SimilarQuestionDto> questionDtos = results.stream()
                .map(this::toDto)
                .toList();

        log.info("Found {} similar questions for JD {}", questionDtos.size(), jdId);

        return SimilarQuestionsResponse.builder()
                .jdId(jdId)
                .totalCount(questionDtos.size())
                .questions(questionDtos)
                .ragEnabled(true)
                .build();
    }

    /**
     * RAG 서비스 가용 여부 확인
     */
    public boolean isRagEnabled() {
        return embeddingService.isAvailable();
    }

    private SimilarQuestionDto toDto(SimilarQuestionResult result) {
        return SimilarQuestionDto.builder()
                .questionId(result.getQuestionId())
                .jdId(result.getJdId())
                .questionType(result.getQuestionType())
                .skillCategory(result.getSkillCategory())
                .content(result.getContent())
                .similarityScore(result.getScore())
                .build();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength
                ? text.substring(0, maxLength) + "..."
                : text;
    }
}
