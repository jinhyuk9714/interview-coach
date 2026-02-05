package com.interviewcoach.question.application.service;

import com.interviewcoach.question.application.dto.request.GenerateQuestionsRequest;
import com.interviewcoach.question.application.dto.response.GeneratedQuestionsResponse;
import com.interviewcoach.question.application.dto.response.QuestionResponse;
import com.interviewcoach.question.domain.entity.GeneratedQuestion;
import com.interviewcoach.question.domain.entity.JobDescription;
import com.interviewcoach.question.domain.repository.GeneratedQuestionRepository;
import com.interviewcoach.question.domain.repository.JobDescriptionRepository;
import com.interviewcoach.question.exception.JdNotFoundException;
import com.interviewcoach.question.infrastructure.llm.LlmClient;
import com.interviewcoach.question.infrastructure.rag.QuestionEmbeddingService;
import com.interviewcoach.question.infrastructure.rag.SimilarQuestionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    private static final int RAG_SIMILAR_QUESTION_LIMIT = 5;

    private final JobDescriptionRepository jdRepository;
    private final GeneratedQuestionRepository questionRepository;
    private final LlmClient llmClient;
    private final QuestionEmbeddingService embeddingService;

    @Transactional
    public GeneratedQuestionsResponse generateQuestions(Long userId, GenerateQuestionsRequest request) {
        JobDescription jd = jdRepository.findById(request.getJdId())
                .orElseThrow(() -> new JdNotFoundException(request.getJdId()));

        // 기존 질문이 없거나 분석된 스킬이 없으면 mock 데이터 사용
        List<String> skills = jd.getParsedSkills();
        if (skills == null || skills.isEmpty()) {
            skills = List.of("Java", "Spring Boot", "JPA");
        }

        String questionType = request.getQuestionType() != null ? request.getQuestionType() : "mixed";

        log.info("Generating questions: jdId={}, type={}, count={}, difficulty={}",
                request.getJdId(), questionType, request.getCount(), request.getDifficulty());

        // RAG: 유사 질문 검색
        List<SimilarQuestionResult> similarQuestions = findSimilarQuestionsForContext(
                jd.getOriginalText(), questionType, skills);

        // LLM으로 질문 생성 (RAG 컨텍스트 활용)
        List<LlmClient.GeneratedQuestionResult> results;
        if (!similarQuestions.isEmpty()) {
            log.info("Found {} similar questions for RAG context", similarQuestions.size());
            results = llmClient.generateQuestionsWithContext(
                    jd.getOriginalText(),
                    skills,
                    questionType,
                    request.getCount(),
                    request.getDifficulty(),
                    similarQuestions
            );
        } else {
            log.info("No similar questions found, using standard generation");
            results = llmClient.generateQuestions(
                    jd.getOriginalText(),
                    skills,
                    questionType,
                    request.getCount(),
                    request.getDifficulty()
            );
        }

        // 기존 질문 삭제 (DB + 임베딩)
        questionRepository.deleteByJdId(jd.getId());
        embeddingService.deleteByJdId(jd.getId());

        // 새 질문 저장 (DB)
        List<GeneratedQuestion> questions = results.stream()
                .map(r -> GeneratedQuestion.builder()
                        .jdId(jd.getId())
                        .questionType(r.questionType())
                        .skillCategory(r.skillCategory())
                        .questionText(r.questionText())
                        .hint(r.hint())
                        .idealAnswer(r.idealAnswer())
                        .difficulty(r.difficulty())
                        .build())
                .toList();

        List<GeneratedQuestion> savedQuestions = questionRepository.saveAll(questions);

        // 새 질문 임베딩 저장 (ChromaDB)
        storeQuestionsToChroma(savedQuestions, jd);

        log.info("Generated {} questions for JD {}", savedQuestions.size(), request.getJdId());

        List<QuestionResponse> questionResponses = savedQuestions.stream()
                .map(QuestionResponse::from)
                .toList();

        return GeneratedQuestionsResponse.builder()
                .jdId(jd.getId())
                .totalCount(questionResponses.size())
                .questions(questionResponses)
                .build();
    }

    /**
     * RAG 컨텍스트용 유사 질문 검색
     */
    private List<SimilarQuestionResult> findSimilarQuestionsForContext(
            String jdText, String questionType, List<String> skills) {
        if (!embeddingService.isAvailable()) {
            log.debug("Embedding service not available, skipping RAG");
            return List.of();
        }

        try {
            return embeddingService.findSimilarQuestions(
                    jdText, questionType, skills, RAG_SIMILAR_QUESTION_LIMIT);
        } catch (Exception e) {
            log.warn("Failed to find similar questions: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 생성된 질문을 ChromaDB에 비동기로 저장
     */
    private void storeQuestionsToChroma(List<GeneratedQuestion> questions, JobDescription jd) {
        if (!embeddingService.isAvailable() || questions.isEmpty()) {
            return;
        }

        try {
            embeddingService.storeQuestions(questions, jd.getCompanyName(), jd.getPosition());
        } catch (Exception e) {
            // 임베딩 저장 실패는 로그만 남기고 진행
            log.error("Failed to store question embeddings: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByJd(Long jdId) {
        return questionRepository.findByJdId(jdId).stream()
                .map(QuestionResponse::from)
                .toList();
    }
}
