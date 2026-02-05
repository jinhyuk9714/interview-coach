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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionGenerationService 단위 테스트")
class QuestionGenerationServiceTest {

    @Mock
    private JobDescriptionRepository jdRepository;

    @Mock
    private GeneratedQuestionRepository questionRepository;

    @Mock
    private LlmClient llmClient;

    @Mock
    private QuestionEmbeddingService embeddingService;

    @InjectMocks
    private QuestionGenerationService questionGenerationService;

    @Captor
    private ArgumentCaptor<List<GeneratedQuestion>> questionsCaptor;

    private static final Long USER_ID = 1L;
    private static final Long JD_ID = 100L;

    @Nested
    @DisplayName("generateQuestions 메서드")
    class GenerateQuestionsTest {

        @Test
        @DisplayName("질문 생성 성공 - 분석된 스킬 있음")
        void generateQuestions_WithParsedSkills_Success() throws Exception {
            // given
            GenerateQuestionsRequest request = createGenerateRequest(JD_ID, "technical", 3, 3);
            JobDescription jd = createJobDescriptionWithSkills(JD_ID, USER_ID,
                    List.of("Java", "Spring Boot", "JPA"));

            List<LlmClient.GeneratedQuestionResult> mockResults = List.of(
                    new LlmClient.GeneratedQuestionResult(
                            "technical", "Java", "Java의 GC에 대해 설명해주세요.",
                            "메모리 관리를 생각해보세요", "GC는 힙 메모리를 관리합니다...", 3),
                    new LlmClient.GeneratedQuestionResult(
                            "technical", "Spring", "Spring DI에 대해 설명해주세요.",
                            "IoC 컨테이너를 생각해보세요", "DI는 의존성 주입으로...", 3)
            );

            List<GeneratedQuestion> savedQuestions = createSavedQuestions(mockResults, JD_ID);

            given(embeddingService.isAvailable()).willReturn(false);
            given(jdRepository.findById(JD_ID)).willReturn(Optional.of(jd));
            given(llmClient.generateQuestions(anyString(), anyList(), anyString(), anyInt(), anyInt()))
                    .willReturn(mockResults);
            given(questionRepository.saveAll(anyList())).willReturn(savedQuestions);

            // when
            GeneratedQuestionsResponse response = questionGenerationService.generateQuestions(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getJdId()).isEqualTo(JD_ID);
            assertThat(response.getTotalCount()).isEqualTo(2);
            assertThat(response.getQuestions()).hasSize(2);

            verify(llmClient, times(1)).generateQuestions(
                    eq(jd.getOriginalText()),
                    eq(List.of("Java", "Spring Boot", "JPA")),
                    eq("technical"),
                    eq(3),
                    eq(3)
            );
        }

        @Test
        @DisplayName("질문 생성 성공 - 분석된 스킬 없음 (기본 스킬 사용)")
        void generateQuestions_WithoutParsedSkills_Success() throws Exception {
            // given
            GenerateQuestionsRequest request = createGenerateRequest(JD_ID, "mixed", 5, 3);
            JobDescription jd = createJobDescriptionWithSkills(JD_ID, USER_ID, null);

            List<LlmClient.GeneratedQuestionResult> mockResults = List.of(
                    new LlmClient.GeneratedQuestionResult(
                            "technical", "Java", "질문 1",
                            "힌트 1", "이상적 답변 1", 3)
            );

            given(embeddingService.isAvailable()).willReturn(false);
            given(jdRepository.findById(JD_ID)).willReturn(Optional.of(jd));
            given(llmClient.generateQuestions(anyString(), anyList(), anyString(), anyInt(), anyInt()))
                    .willReturn(mockResults);
            given(questionRepository.saveAll(anyList())).willReturn(createSavedQuestions(mockResults, JD_ID));

            // when
            questionGenerationService.generateQuestions(USER_ID, request);

            // then
            verify(llmClient, times(1)).generateQuestions(
                    anyString(),
                    eq(List.of("Java", "Spring Boot", "JPA")), // 기본 스킬
                    eq("mixed"),
                    anyInt(),
                    anyInt()
            );
        }

        @Test
        @DisplayName("존재하지 않는 JD로 질문 생성 시 예외 발생")
        void generateQuestions_JdNotFound() throws Exception {
            // given
            GenerateQuestionsRequest request = createGenerateRequest(JD_ID, "technical", 5, 3);
            given(jdRepository.findById(JD_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> questionGenerationService.generateQuestions(USER_ID, request))
                    .isInstanceOf(JdNotFoundException.class);
            verify(llmClient, never()).generateQuestions(any(), any(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("questionType이 null일 때 'mixed' 사용")
        void generateQuestions_NullQuestionType_UseMixed() throws Exception {
            // given
            GenerateQuestionsRequest request = createGenerateRequest(JD_ID, null, 5, 3);
            JobDescription jd = createJobDescriptionWithSkills(JD_ID, USER_ID, List.of("Java"));

            List<LlmClient.GeneratedQuestionResult> mockResults = List.of(
                    new LlmClient.GeneratedQuestionResult(
                            "mixed", "Java", "질문", "힌트", "답변", 3)
            );

            given(embeddingService.isAvailable()).willReturn(false);
            given(jdRepository.findById(JD_ID)).willReturn(Optional.of(jd));
            given(llmClient.generateQuestions(anyString(), anyList(), anyString(), anyInt(), anyInt()))
                    .willReturn(mockResults);
            given(questionRepository.saveAll(anyList())).willReturn(createSavedQuestions(mockResults, JD_ID));

            // when
            questionGenerationService.generateQuestions(USER_ID, request);

            // then
            verify(llmClient).generateQuestions(anyString(), anyList(), eq("mixed"), anyInt(), anyInt());
        }
    }

    @Nested
    @DisplayName("getQuestionsByJd 메서드")
    class GetQuestionsByJdTest {

        @Test
        @DisplayName("JD별 질문 목록 조회 성공")
        void getQuestionsByJd_Success() throws Exception {
            // given
            List<GeneratedQuestion> questions = List.of(
                    createGeneratedQuestion(1L, JD_ID, "technical", "Java", "질문 1", 3),
                    createGeneratedQuestion(2L, JD_ID, "behavioral", "Soft Skills", "질문 2", 2)
            );
            given(questionRepository.findByJdId(JD_ID)).willReturn(questions);

            // when
            List<QuestionResponse> responses = questionGenerationService.getQuestionsByJd(JD_ID);

            // then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getQuestionText()).isEqualTo("질문 1");
            assertThat(responses.get(1).getQuestionText()).isEqualTo("질문 2");
        }

        @Test
        @DisplayName("질문이 없는 JD 조회 시 빈 목록 반환")
        void getQuestionsByJd_Empty() {
            // given
            given(questionRepository.findByJdId(JD_ID)).willReturn(List.of());

            // when
            List<QuestionResponse> responses = questionGenerationService.getQuestionsByJd(JD_ID);

            // then
            assertThat(responses).isEmpty();
        }
    }

    // Helper methods
    private GenerateQuestionsRequest createGenerateRequest(Long jdId, String questionType, int count, int difficulty) throws Exception {
        GenerateQuestionsRequest request = new GenerateQuestionsRequest();
        setField(request, "jdId", jdId);
        setField(request, "questionType", questionType);
        setField(request, "count", count);
        setField(request, "difficulty", difficulty);
        return request;
    }

    private JobDescription createJobDescriptionWithSkills(Long id, Long userId, List<String> skills) throws Exception {
        JobDescription jd = JobDescription.builder()
                .userId(userId)
                .companyName("테스트 회사")
                .position("백엔드 개발자")
                .originalText("JD 내용")
                .parsedSkills(skills != null ? new ArrayList<>(skills) : new ArrayList<>())
                .build();
        setField(jd, "id", id);
        return jd;
    }

    private List<GeneratedQuestion> createSavedQuestions(List<LlmClient.GeneratedQuestionResult> results, Long jdId) throws Exception {
        List<GeneratedQuestion> questions = new ArrayList<>();
        long id = 1L;
        for (LlmClient.GeneratedQuestionResult r : results) {
            GeneratedQuestion q = GeneratedQuestion.builder()
                    .jdId(jdId)
                    .questionType(r.questionType())
                    .skillCategory(r.skillCategory())
                    .questionText(r.questionText())
                    .hint(r.hint())
                    .idealAnswer(r.idealAnswer())
                    .difficulty(r.difficulty())
                    .build();
            setField(q, "id", id++);
            setField(q, "createdAt", LocalDateTime.now());
            questions.add(q);
        }
        return questions;
    }

    private GeneratedQuestion createGeneratedQuestion(Long id, Long jdId, String questionType,
                                                       String skillCategory, String questionText, int difficulty) throws Exception {
        GeneratedQuestion question = GeneratedQuestion.builder()
                .jdId(jdId)
                .questionType(questionType)
                .skillCategory(skillCategory)
                .questionText(questionText)
                .hint("힌트")
                .idealAnswer("이상적 답변")
                .difficulty(difficulty)
                .build();
        setField(question, "id", id);
        setField(question, "createdAt", LocalDateTime.now());
        return question;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
