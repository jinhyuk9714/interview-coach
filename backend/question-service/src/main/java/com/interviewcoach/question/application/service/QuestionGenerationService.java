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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    private final JobDescriptionRepository jdRepository;
    private final GeneratedQuestionRepository questionRepository;
    private final LlmClient llmClient;

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

        // LLM으로 질문 생성
        List<LlmClient.GeneratedQuestionResult> results = llmClient.generateQuestions(
                jd.getOriginalText(),
                skills,
                questionType,
                request.getCount(),
                request.getDifficulty()
        );

        // 기존 질문 삭제 후 새 질문 저장
        questionRepository.deleteByJdId(jd.getId());
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

    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByJd(Long jdId) {
        return questionRepository.findByJdId(jdId).stream()
                .map(QuestionResponse::from)
                .toList();
    }
}
