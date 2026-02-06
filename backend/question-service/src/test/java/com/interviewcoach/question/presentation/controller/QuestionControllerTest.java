package com.interviewcoach.question.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.question.application.dto.response.GeneratedQuestionsResponse;
import com.interviewcoach.question.application.dto.response.QuestionResponse;
import com.interviewcoach.question.application.dto.response.SimilarQuestionDto;
import com.interviewcoach.question.application.dto.response.SimilarQuestionsResponse;
import com.interviewcoach.question.application.service.QuestionGenerationService;
import com.interviewcoach.question.application.service.SimilarQuestionService;
import com.interviewcoach.question.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("QuestionController 통합 테스트")
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuestionGenerationService questionGenerationService;

    @MockBean
    private SimilarQuestionService similarQuestionService;

    private static final Long USER_ID = 1L;
    private static final Long JD_ID = 10L;

    @Nested
    @DisplayName("POST /api/v1/questions/generate - 질문 생성")
    class GenerateQuestionsTest {

        @Test
        @DisplayName("질문 생성 성공 - 201 Created")
        void generateQuestions_Success() throws Exception {
            // given
            GeneratedQuestionsResponse response = GeneratedQuestionsResponse.builder()
                    .jdId(JD_ID)
                    .totalCount(3)
                    .questions(List.of(
                            QuestionResponse.builder()
                                    .id(1L)
                                    .jdId(JD_ID)
                                    .questionType("technical")
                                    .skillCategory("Java")
                                    .questionText("Java의 GC에 대해 설명해주세요.")
                                    .difficulty(3)
                                    .build(),
                            QuestionResponse.builder()
                                    .id(2L)
                                    .jdId(JD_ID)
                                    .questionType("technical")
                                    .skillCategory("Spring")
                                    .questionText("Spring DI란 무엇인가요?")
                                    .difficulty(3)
                                    .build(),
                            QuestionResponse.builder()
                                    .id(3L)
                                    .jdId(JD_ID)
                                    .questionType("behavioral")
                                    .skillCategory("협업")
                                    .questionText("팀 갈등을 해결한 경험이 있나요?")
                                    .difficulty(2)
                                    .build()
                    ))
                    .build();

            given(questionGenerationService.generateQuestions(eq(USER_ID), any())).willReturn(response);

            String requestBody = """
                {
                    "jdId": 10,
                    "questionType": "mixed",
                    "count": 3,
                    "difficulty": 3
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.jdId").value(JD_ID))
                    .andExpect(jsonPath("$.totalCount").value(3))
                    .andExpect(jsonPath("$.questions").isArray())
                    .andExpect(jsonPath("$.questions.length()").value(3))
                    .andExpect(jsonPath("$.questions[0].questionType").value("technical"))
                    .andExpect(jsonPath("$.questions[0].skillCategory").value("Java"));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러 - jdId")
        void generateQuestions_MissingJdId() throws Exception {
            String requestBody = """
                {
                    "questionType": "mixed",
                    "count": 3
                }
                """;

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/questions/jd/{jdId} - JD별 질문 목록 조회")
    class GetQuestionsByJdTest {

        @Test
        @DisplayName("JD별 질문 목록 조회 성공 - 200 OK")
        void getQuestionsByJd_Success() throws Exception {
            // given
            List<QuestionResponse> response = List.of(
                    QuestionResponse.builder()
                            .id(1L)
                            .jdId(JD_ID)
                            .questionType("technical")
                            .skillCategory("Java")
                            .questionText("Java의 GC에 대해 설명해주세요.")
                            .difficulty(3)
                            .build(),
                    QuestionResponse.builder()
                            .id(2L)
                            .jdId(JD_ID)
                            .questionType("behavioral")
                            .skillCategory("협업")
                            .questionText("팀워크 경험을 말해주세요.")
                            .difficulty(2)
                            .build()
            );

            given(questionGenerationService.getQuestionsByJd(JD_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/questions/jd/{jdId}", JD_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].questionType").value("technical"))
                    .andExpect(jsonPath("$[1].questionType").value("behavioral"));
        }

        @Test
        @DisplayName("질문이 없는 JD - 빈 목록 반환")
        void getQuestionsByJd_Empty() throws Exception {
            // given
            given(questionGenerationService.getQuestionsByJd(JD_ID)).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/v1/questions/jd/{jdId}", JD_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/questions/similar - 유사 질문 검색")
    class SearchSimilarQuestionsTest {

        @Test
        @DisplayName("유사 질문 검색 성공 - 200 OK")
        void searchSimilarQuestions_Success() throws Exception {
            // given
            SimilarQuestionsResponse response = SimilarQuestionsResponse.builder()
                    .query("Java Spring Boot 백엔드")
                    .totalCount(2)
                    .questions(List.of(
                            SimilarQuestionDto.builder()
                                    .questionId(1L)
                                    .jdId(5L)
                                    .questionType("technical")
                                    .skillCategory("Java")
                                    .content("Java Stream API에 대해 설명해주세요.")
                                    .similarityScore(0.92)
                                    .build(),
                            SimilarQuestionDto.builder()
                                    .questionId(2L)
                                    .jdId(7L)
                                    .questionType("technical")
                                    .skillCategory("Spring")
                                    .content("Spring Boot 자동 설정 원리를 설명해주세요.")
                                    .similarityScore(0.87)
                                    .build()
                    ))
                    .ragEnabled(true)
                    .build();

            given(similarQuestionService.searchByQuery(eq("Java Spring Boot 백엔드"), isNull(), isNull(), eq(5)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/questions/similar")
                            .param("query", "Java Spring Boot 백엔드"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.query").value("Java Spring Boot 백엔드"))
                    .andExpect(jsonPath("$.totalCount").value(2))
                    .andExpect(jsonPath("$.questions").isArray())
                    .andExpect(jsonPath("$.questions.length()").value(2))
                    .andExpect(jsonPath("$.questions[0].similarityScore").value(0.92))
                    .andExpect(jsonPath("$.ragEnabled").value(true));
        }

        @Test
        @DisplayName("query 파라미터 누락 시 에러 응답")
        void searchSimilarQuestions_MissingQuery() throws Exception {
            // when & then - MissingServletRequestParameterException
            mockMvc.perform(get("/api/v1/questions/similar"))
                    .andExpect(result -> assertThat(result.getResponse().getStatus())
                            .isGreaterThanOrEqualTo(400));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/questions/rag/status - RAG 상태 확인")
    class RagStatusTest {

        @Test
        @DisplayName("RAG 활성화 상태 확인 - 200 OK")
        void getRagStatus_Enabled() throws Exception {
            // given
            given(similarQuestionService.isRagEnabled()).willReturn(true);

            // when & then
            mockMvc.perform(get("/api/v1/questions/rag/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ragEnabled").value(true))
                    .andExpect(jsonPath("$.embeddingModel").value("AllMiniLmL6V2"))
                    .andExpect(jsonPath("$.vectorStore").value("ChromaDB"));
        }

        @Test
        @DisplayName("RAG 비활성화 상태 확인")
        void getRagStatus_Disabled() throws Exception {
            // given
            given(similarQuestionService.isRagEnabled()).willReturn(false);

            // when & then
            mockMvc.perform(get("/api/v1/questions/rag/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ragEnabled").value(false));
        }
    }
}
