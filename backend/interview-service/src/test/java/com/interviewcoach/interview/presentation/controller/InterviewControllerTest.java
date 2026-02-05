package com.interviewcoach.interview.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.interview.application.dto.request.StartInterviewRequest;
import com.interviewcoach.interview.application.dto.request.SubmitAnswerRequest;
import com.interviewcoach.interview.application.dto.response.InterviewListResponse;
import com.interviewcoach.interview.application.dto.response.InterviewSessionResponse;
import com.interviewcoach.interview.application.dto.response.QnaResponse;
import com.interviewcoach.interview.application.service.InterviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterviewController.class)
@DisplayName("InterviewController 통합 테스트")
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InterviewService interviewService;

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 100L;

    @Nested
    @DisplayName("POST /api/v1/interviews - 면접 시작")
    class StartInterviewTest {

        @Test
        @DisplayName("면접 세션 시작 성공")
        void startInterview_Success() throws Exception {
            // given
            InterviewSessionResponse response = InterviewSessionResponse.builder()
                    .id(SESSION_ID)
                    .userId(USER_ID)
                    .jdId(1L)
                    .interviewType("mixed")
                    .status("IN_PROGRESS")
                    .totalQuestions(5)
                    .startedAt(LocalDateTime.now())
                    .qnaList(List.of(
                            QnaResponse.builder()
                                    .id(1L)
                                    .questionOrder(1)
                                    .questionType("technical")
                                    .questionText("Java의 GC에 대해 설명해주세요.")
                                    .build()
                    ))
                    .build();

            given(interviewService.startInterview(eq(USER_ID), any(StartInterviewRequest.class)))
                    .willReturn(response);

            String requestBody = """
                {
                    "jdId": 1,
                    "interviewType": "mixed",
                    "questions": [
                        {"questionType": "technical", "questionText": "Java의 GC에 대해 설명해주세요."}
                    ]
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/interviews")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SESSION_ID))
                    .andExpect(jsonPath("$.userId").value(USER_ID))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.totalQuestions").value(5));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러")
        void startInterview_MissingJdId() throws Exception {
            String requestBody = """
                {
                    "interviewType": "mixed",
                    "questions": []
                }
                """;

            mockMvc.perform(post("/api/v1/interviews")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/interviews - 면접 목록 조회 (대시보드)")
    class GetInterviewsTest {

        @Test
        @DisplayName("면접 목록 조회 성공")
        void getInterviews_Success() throws Exception {
            // given
            InterviewListResponse response = InterviewListResponse.builder()
                    .totalCount(3)
                    .interviews(List.of(
                            InterviewSessionResponse.builder()
                                    .id(1L)
                                    .userId(USER_ID)
                                    .jdId(1L)
                                    .interviewType("technical")
                                    .status("COMPLETED")
                                    .totalQuestions(5)
                                    .avgScore(BigDecimal.valueOf(85))
                                    .startedAt(LocalDateTime.now().minusDays(1))
                                    .completedAt(LocalDateTime.now().minusDays(1))
                                    .build(),
                            InterviewSessionResponse.builder()
                                    .id(2L)
                                    .userId(USER_ID)
                                    .jdId(2L)
                                    .interviewType("behavioral")
                                    .status("COMPLETED")
                                    .totalQuestions(5)
                                    .avgScore(BigDecimal.valueOf(78))
                                    .startedAt(LocalDateTime.now().minusDays(2))
                                    .completedAt(LocalDateTime.now().minusDays(2))
                                    .build(),
                            InterviewSessionResponse.builder()
                                    .id(3L)
                                    .userId(USER_ID)
                                    .jdId(1L)
                                    .interviewType("mixed")
                                    .status("IN_PROGRESS")
                                    .totalQuestions(5)
                                    .startedAt(LocalDateTime.now())
                                    .build()
                    ))
                    .build();

            given(interviewService.getInterviews(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/interviews")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(3))
                    .andExpect(jsonPath("$.interviews").isArray())
                    .andExpect(jsonPath("$.interviews.length()").value(3))
                    .andExpect(jsonPath("$.interviews[0].id").value(1))
                    .andExpect(jsonPath("$.interviews[0].avgScore").value(85))
                    .andExpect(jsonPath("$.interviews[0].status").value("COMPLETED"))
                    .andExpect(jsonPath("$.interviews[2].status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("면접 기록이 없는 사용자")
        void getInterviews_Empty() throws Exception {
            // given
            InterviewListResponse response = InterviewListResponse.builder()
                    .totalCount(0)
                    .interviews(List.of())
                    .build();

            given(interviewService.getInterviews(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/interviews")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(0))
                    .andExpect(jsonPath("$.interviews").isEmpty());
        }

        @Test
        @DisplayName("X-User-Id 헤더 누락 시 에러")
        void getInterviews_MissingHeader() throws Exception {
            // Spring에서 필수 헤더 누락 시 500 반환 (글로벌 예외 처리기에서 처리)
            mockMvc.perform(get("/api/v1/interviews"))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/interviews/{id} - 면접 상세 조회")
    class GetInterviewTest {

        @Test
        @DisplayName("면접 상세 조회 성공")
        void getInterview_Success() throws Exception {
            // given
            InterviewSessionResponse response = InterviewSessionResponse.builder()
                    .id(SESSION_ID)
                    .userId(USER_ID)
                    .jdId(1L)
                    .interviewType("technical")
                    .status("COMPLETED")
                    .totalQuestions(3)
                    .avgScore(BigDecimal.valueOf(80))
                    .startedAt(LocalDateTime.now().minusHours(1))
                    .completedAt(LocalDateTime.now())
                    .qnaList(List.of(
                            QnaResponse.builder()
                                    .id(1L)
                                    .questionOrder(1)
                                    .questionType("technical")
                                    .questionText("Java의 GC에 대해 설명해주세요.")
                                    .answerText("GC는 가비지 컬렉션으로...")
                                    .feedback(Map.of("score", 85))
                                    .build(),
                            QnaResponse.builder()
                                    .id(2L)
                                    .questionOrder(2)
                                    .questionType("technical")
                                    .questionText("Spring DI란 무엇인가요?")
                                    .answerText("DI는 의존성 주입으로...")
                                    .feedback(Map.of("score", 75))
                                    .build()
                    ))
                    .build();

            given(interviewService.getInterview(SESSION_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/interviews/{id}", SESSION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SESSION_ID))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.avgScore").value(80))
                    .andExpect(jsonPath("$.qnaList").isArray())
                    .andExpect(jsonPath("$.qnaList.length()").value(2))
                    .andExpect(jsonPath("$.qnaList[0].feedback.score").value(85));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/interviews/{id}/answer - 답변 제출")
    class SubmitAnswerTest {

        @Test
        @DisplayName("답변 제출 성공")
        void submitAnswer_Success() throws Exception {
            // given
            QnaResponse response = QnaResponse.builder()
                    .id(1L)
                    .questionOrder(1)
                    .questionType("technical")
                    .questionText("Java의 GC에 대해 설명해주세요.")
                    .answerText("GC는 가비지 컬렉션으로...")
                    .answeredAt(LocalDateTime.now())
                    .build();

            given(interviewService.submitAnswer(eq(SESSION_ID), any(SubmitAnswerRequest.class)))
                    .willReturn(response);

            String requestBody = """
                {
                    "questionOrder": 1,
                    "answerText": "GC는 가비지 컬렉션으로..."
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/interviews/{id}/answer", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionOrder").value(1))
                    .andExpect(jsonPath("$.answerText").value("GC는 가비지 컬렉션으로..."));
        }

        @Test
        @DisplayName("빈 답변 제출 시 400 에러")
        void submitAnswer_EmptyAnswer() throws Exception {
            String requestBody = """
                {
                    "questionOrder": 1,
                    "answerText": ""
                }
                """;

            mockMvc.perform(post("/api/v1/interviews/{id}/answer", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/interviews/{id}/complete - 면접 완료")
    class CompleteInterviewTest {

        @Test
        @DisplayName("면접 완료 성공")
        void completeInterview_Success() throws Exception {
            // given
            InterviewSessionResponse response = InterviewSessionResponse.builder()
                    .id(SESSION_ID)
                    .userId(USER_ID)
                    .jdId(1L)
                    .interviewType("mixed")
                    .status("COMPLETED")
                    .totalQuestions(5)
                    .avgScore(BigDecimal.valueOf(82))
                    .startedAt(LocalDateTime.now().minusMinutes(30))
                    .completedAt(LocalDateTime.now())
                    .build();

            given(interviewService.completeInterview(SESSION_ID)).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/interviews/{id}/complete", SESSION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.avgScore").value(82))
                    .andExpect(jsonPath("$.completedAt").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/interviews/{id}/qna/{questionOrder}/feedback - 피드백 업데이트")
    class UpdateFeedbackTest {

        @Test
        @DisplayName("피드백 업데이트 성공")
        void updateFeedback_Success() throws Exception {
            // given
            QnaResponse response = QnaResponse.builder()
                    .id(1L)
                    .questionOrder(1)
                    .questionType("technical")
                    .questionText("Java의 GC에 대해 설명해주세요.")
                    .answerText("GC는 가비지 컬렉션으로...")
                    .feedback(Map.of(
                            "score", 85,
                            "strengths", List.of("핵심 개념을 잘 설명했습니다"),
                            "improvements", List.of("더 구체적인 예시를 들어보세요")
                    ))
                    .build();

            given(interviewService.updateFeedback(eq(SESSION_ID), eq(1), any(Map.class)))
                    .willReturn(response);

            String requestBody = """
                {
                    "score": 85,
                    "strengths": ["핵심 개념을 잘 설명했습니다"],
                    "improvements": ["더 구체적인 예시를 들어보세요"],
                    "tips": ["STAR 기법을 활용해보세요"]
                }
                """;

            // when & then
            mockMvc.perform(put("/api/v1/interviews/{id}/qna/{questionOrder}/feedback", SESSION_ID, 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.feedback.score").value(85))
                    .andExpect(jsonPath("$.feedback.strengths[0]").value("핵심 개념을 잘 설명했습니다"));
        }
    }

    @Nested
    @DisplayName("대시보드 시나리오 테스트")
    class DashboardScenarioTest {

        @Test
        @DisplayName("대시보드 - 최근 면접 3개 조회")
        void dashboard_RecentInterviews() throws Exception {
            // given - 대시보드에서 보여줄 최근 면접 목록
            InterviewListResponse response = InterviewListResponse.builder()
                    .totalCount(10) // 총 10개 중
                    .interviews(List.of(
                            InterviewSessionResponse.builder()
                                    .id(10L)
                                    .jdId(1L)
                                    .interviewType("technical")
                                    .status("COMPLETED")
                                    .totalQuestions(5)
                                    .avgScore(BigDecimal.valueOf(92))
                                    .startedAt(LocalDateTime.now().minusHours(1))
                                    .completedAt(LocalDateTime.now())
                                    .build(),
                            InterviewSessionResponse.builder()
                                    .id(9L)
                                    .jdId(2L)
                                    .interviewType("behavioral")
                                    .status("COMPLETED")
                                    .totalQuestions(5)
                                    .avgScore(BigDecimal.valueOf(78))
                                    .startedAt(LocalDateTime.now().minusDays(1))
                                    .completedAt(LocalDateTime.now().minusDays(1))
                                    .build(),
                            InterviewSessionResponse.builder()
                                    .id(8L)
                                    .jdId(1L)
                                    .interviewType("mixed")
                                    .status("COMPLETED")
                                    .totalQuestions(5)
                                    .avgScore(BigDecimal.valueOf(85))
                                    .startedAt(LocalDateTime.now().minusDays(2))
                                    .completedAt(LocalDateTime.now().minusDays(2))
                                    .build()
                    ))
                    .build();

            given(interviewService.getInterviews(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/interviews")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalCount").value(10))
                    .andExpect(jsonPath("$.interviews.length()").value(3))
                    // 첫 번째가 가장 최근 (가장 높은 점수)
                    .andExpect(jsonPath("$.interviews[0].avgScore").value(92))
                    // 두 번째
                    .andExpect(jsonPath("$.interviews[1].avgScore").value(78))
                    // 세 번째
                    .andExpect(jsonPath("$.interviews[2].avgScore").value(85));
        }

        @Test
        @DisplayName("대시보드 - 진행 중인 면접 표시")
        void dashboard_InProgressInterview() throws Exception {
            // given - 진행 중인 면접이 있는 경우
            InterviewListResponse response = InterviewListResponse.builder()
                    .totalCount(2)
                    .interviews(List.of(
                            InterviewSessionResponse.builder()
                                    .id(5L)
                                    .jdId(1L)
                                    .interviewType("technical")
                                    .status("IN_PROGRESS")
                                    .totalQuestions(5)
                                    .startedAt(LocalDateTime.now().minusMinutes(10))
                                    .build(),
                            InterviewSessionResponse.builder()
                                    .id(4L)
                                    .jdId(1L)
                                    .interviewType("mixed")
                                    .status("COMPLETED")
                                    .totalQuestions(5)
                                    .avgScore(BigDecimal.valueOf(75))
                                    .startedAt(LocalDateTime.now().minusDays(1))
                                    .completedAt(LocalDateTime.now().minusDays(1))
                                    .build()
                    ))
                    .build();

            given(interviewService.getInterviews(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/interviews")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.interviews[0].status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.interviews[0].avgScore").isEmpty())
                    .andExpect(jsonPath("$.interviews[1].status").value("COMPLETED"))
                    .andExpect(jsonPath("$.interviews[1].avgScore").value(75));
        }
    }
}
