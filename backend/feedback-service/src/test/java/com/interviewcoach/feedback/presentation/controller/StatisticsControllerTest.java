package com.interviewcoach.feedback.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.feedback.application.dto.request.RecordAnswerRequest;
import com.interviewcoach.feedback.application.dto.response.StatisticsResponse;
import com.interviewcoach.feedback.application.dto.response.UserStatisticsSummaryResponse;
import com.interviewcoach.feedback.application.dto.response.UserStatisticsSummaryResponse.*;
import com.interviewcoach.feedback.application.service.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatisticsController.class)
@DisplayName("StatisticsController 통합 테스트")
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatisticsService statisticsService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("GET /api/v1/statistics")
    class GetStatisticsTest {

        @Test
        @DisplayName("사용자 통계 조회 성공")
        void getStatistics_Success() throws Exception {
            // given
            UserStatisticsSummaryResponse response = UserStatisticsSummaryResponse.builder()
                    .userId(USER_ID)
                    .totalQuestions(20)
                    .totalCorrect(15)
                    .overallCorrectRate(BigDecimal.valueOf(75))
                    .totalInterviews(4)
                    .avgScore(75)
                    .scoreTrend(5)
                    .streakDays(3)
                    .rank("B")
                    .byCategory(List.of(
                            StatisticsResponse.builder()
                                    .userId(USER_ID)
                                    .skillCategory("Java")
                                    .totalQuestions(10)
                                    .correctCount(8)
                                    .correctRate(BigDecimal.valueOf(80))
                                    .build()
                    ))
                    .categoryStats(List.of(
                            CategoryStatResponse.builder()
                                    .category("Java")
                                    .score(80)
                                    .total(10)
                                    .trend(2)
                                    .build()
                    ))
                    .weeklyActivity(List.of(
                            WeeklyActivityResponse.builder()
                                    .day("월")
                                    .count(2)
                                    .score(75)
                                    .build()
                    ))
                    .recentProgress(List.of(
                            ProgressPointResponse.builder()
                                    .date("2/1")
                                    .score(70)
                                    .build()
                    ))
                    .weakPoints(List.of(
                            WeakPointResponse.builder()
                                    .skill("Database")
                                    .score(55)
                                    .suggestion("SQL 최적화를 연습하세요")
                                    .build()
                    ))
                    .build();

            given(statisticsService.getStatistics(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/statistics")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(USER_ID))
                    .andExpect(jsonPath("$.totalQuestions").value(20))
                    .andExpect(jsonPath("$.totalCorrect").value(15))
                    .andExpect(jsonPath("$.avgScore").value(75))
                    .andExpect(jsonPath("$.rank").value("B"))
                    .andExpect(jsonPath("$.categoryStats[0].category").value("Java"))
                    .andExpect(jsonPath("$.categoryStats[0].score").value(80))
                    .andExpect(jsonPath("$.weakPoints[0].skill").value("Database"));
        }

        @Test
        @DisplayName("통계가 없는 신규 사용자 조회")
        void getStatistics_NewUser() throws Exception {
            // given
            UserStatisticsSummaryResponse response = UserStatisticsSummaryResponse.builder()
                    .userId(USER_ID)
                    .totalQuestions(0)
                    .totalCorrect(0)
                    .overallCorrectRate(BigDecimal.ZERO)
                    .totalInterviews(0)
                    .avgScore(0)
                    .scoreTrend(0)
                    .streakDays(0)
                    .rank("-")
                    .byCategory(List.of())
                    .categoryStats(List.of())
                    .weeklyActivity(List.of())
                    .recentProgress(List.of())
                    .weakPoints(List.of())
                    .build();

            given(statisticsService.getStatistics(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/statistics")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalQuestions").value(0))
                    .andExpect(jsonPath("$.avgScore").value(0))
                    .andExpect(jsonPath("$.rank").value("-"))
                    .andExpect(jsonPath("$.categoryStats").isEmpty());
        }

        @Test
        @DisplayName("X-User-Id 헤더 누락 시 에러")
        void getStatistics_MissingHeader() throws Exception {
            // when & then
            // Spring에서 필수 헤더 누락 시 500 또는 400 반환 (구현에 따라 다름)
            mockMvc.perform(get("/api/v1/statistics"))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/statistics/{category}")
    class GetStatisticsByCategoryTest {

        @Test
        @DisplayName("특정 카테고리 통계 조회 성공")
        void getStatisticsByCategory_Success() throws Exception {
            // given
            StatisticsResponse response = StatisticsResponse.builder()
                    .userId(USER_ID)
                    .skillCategory("Java")
                    .totalQuestions(10)
                    .correctCount(8)
                    .correctRate(BigDecimal.valueOf(80))
                    .weakPoints(List.of("상속", "다형성"))
                    .build();

            given(statisticsService.getStatisticsByCategory(USER_ID, "Java")).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/statistics/Java")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skillCategory").value("Java"))
                    .andExpect(jsonPath("$.totalQuestions").value(10))
                    .andExpect(jsonPath("$.correctCount").value(8))
                    .andExpect(jsonPath("$.weakPoints[0]").value("상속"));
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 조회 - 기본값 반환")
        void getStatisticsByCategory_NotFound() throws Exception {
            // given
            StatisticsResponse response = StatisticsResponse.builder()
                    .userId(USER_ID)
                    .skillCategory("Unknown")
                    .totalQuestions(0)
                    .correctCount(0)
                    .correctRate(BigDecimal.ZERO)
                    .build();

            given(statisticsService.getStatisticsByCategory(USER_ID, "Unknown")).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/statistics/Unknown")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skillCategory").value("Unknown"))
                    .andExpect(jsonPath("$.totalQuestions").value(0));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/statistics/record")
    class RecordAnswerTest {

        @Test
        @DisplayName("정답 기록 성공 - 점수 포함")
        void recordAnswer_Success_WithScore() throws Exception {
            // given
            StatisticsResponse response = StatisticsResponse.builder()
                    .userId(USER_ID)
                    .skillCategory("Java")
                    .totalQuestions(5)
                    .correctCount(4)
                    .correctRate(BigDecimal.valueOf(78))
                    .build();

            given(statisticsService.recordAnswer(eq(USER_ID), any(RecordAnswerRequest.class)))
                    .willReturn(response);

            String requestBody = """
                {
                    "skillCategory": "Java",
                    "isCorrect": true,
                    "score": 78
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/statistics/record")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.skillCategory").value("Java"))
                    .andExpect(jsonPath("$.totalQuestions").value(5))
                    .andExpect(jsonPath("$.correctRate").value(78));
        }

        @Test
        @DisplayName("오답 기록 - 취약점 포함")
        void recordAnswer_Incorrect_WithWeakPoint() throws Exception {
            // given
            StatisticsResponse response = StatisticsResponse.builder()
                    .userId(USER_ID)
                    .skillCategory("Database")
                    .totalQuestions(3)
                    .correctCount(1)
                    .correctRate(BigDecimal.valueOf(45))
                    .weakPoints(List.of("SQL 최적화"))
                    .build();

            given(statisticsService.recordAnswer(eq(USER_ID), any(RecordAnswerRequest.class)))
                    .willReturn(response);

            String requestBody = """
                {
                    "skillCategory": "Database",
                    "isCorrect": false,
                    "score": 45,
                    "weakPoint": "SQL 최적화"
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/statistics/record")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correctRate").value(45))
                    .andExpect(jsonPath("$.weakPoints[0]").value("SQL 최적화"));
        }

        @Test
        @DisplayName("점수 없이 기록 - 정답은 100점으로 처리")
        void recordAnswer_NoScore_Correct() throws Exception {
            // given
            StatisticsResponse response = StatisticsResponse.builder()
                    .userId(USER_ID)
                    .skillCategory("Spring")
                    .totalQuestions(1)
                    .correctCount(1)
                    .correctRate(BigDecimal.valueOf(100))
                    .build();

            given(statisticsService.recordAnswer(eq(USER_ID), any(RecordAnswerRequest.class)))
                    .willReturn(response);

            String requestBody = """
                {
                    "skillCategory": "Spring",
                    "isCorrect": true
                }
                """;

            // when & then
            mockMvc.perform(post("/api/v1/statistics/record")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correctRate").value(100));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러 - skillCategory")
        void recordAnswer_MissingSkillCategory() throws Exception {
            String requestBody = """
                {
                    "isCorrect": true,
                    "score": 80
                }
                """;

            mockMvc.perform(post("/api/v1/statistics/record")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 에러 - isCorrect")
        void recordAnswer_MissingIsCorrect() throws Exception {
            String requestBody = """
                {
                    "skillCategory": "Java",
                    "score": 80
                }
                """;

            mockMvc.perform(post("/api/v1/statistics/record")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("빈 skillCategory 시 400 에러")
        void recordAnswer_EmptySkillCategory() throws Exception {
            String requestBody = """
                {
                    "skillCategory": "",
                    "isCorrect": true
                }
                """;

            mockMvc.perform(post("/api/v1/statistics/record")
                            .header("X-User-Id", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("랭크 계산 시나리오")
    class RankScenarioTest {

        @Test
        @DisplayName("S랭크 - 90점 이상")
        void rank_S() throws Exception {
            // given
            UserStatisticsSummaryResponse response = createResponseWithRank(95, "S");
            given(statisticsService.getStatistics(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/statistics")
                            .header("X-User-Id", USER_ID))
                    .andExpect(jsonPath("$.avgScore").value(95))
                    .andExpect(jsonPath("$.rank").value("S"));
        }

        @Test
        @DisplayName("A랭크 - 80점 이상 90점 미만")
        void rank_A() throws Exception {
            // given
            UserStatisticsSummaryResponse response = createResponseWithRank(85, "A");
            given(statisticsService.getStatistics(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/statistics")
                            .header("X-User-Id", USER_ID))
                    .andExpect(jsonPath("$.avgScore").value(85))
                    .andExpect(jsonPath("$.rank").value("A"));
        }

        @Test
        @DisplayName("B랭크 - 70점 이상 80점 미만")
        void rank_B() throws Exception {
            // given
            UserStatisticsSummaryResponse response = createResponseWithRank(75, "B");
            given(statisticsService.getStatistics(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/statistics")
                            .header("X-User-Id", USER_ID))
                    .andExpect(jsonPath("$.avgScore").value(75))
                    .andExpect(jsonPath("$.rank").value("B"));
        }

        @Test
        @DisplayName("C랭크 - 60점 이상 70점 미만")
        void rank_C() throws Exception {
            // given
            UserStatisticsSummaryResponse response = createResponseWithRank(65, "C");
            given(statisticsService.getStatistics(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/statistics")
                            .header("X-User-Id", USER_ID))
                    .andExpect(jsonPath("$.avgScore").value(65))
                    .andExpect(jsonPath("$.rank").value("C"));
        }

        @Test
        @DisplayName("D랭크 - 50점 이상 60점 미만")
        void rank_D() throws Exception {
            // given
            UserStatisticsSummaryResponse response = createResponseWithRank(55, "D");
            given(statisticsService.getStatistics(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/statistics")
                            .header("X-User-Id", USER_ID))
                    .andExpect(jsonPath("$.avgScore").value(55))
                    .andExpect(jsonPath("$.rank").value("D"));
        }

        private UserStatisticsSummaryResponse createResponseWithRank(int avgScore, String rank) {
            return UserStatisticsSummaryResponse.builder()
                    .userId(USER_ID)
                    .totalQuestions(10)
                    .totalCorrect(avgScore / 10)
                    .overallCorrectRate(BigDecimal.valueOf(avgScore))
                    .totalInterviews(2)
                    .avgScore(avgScore)
                    .scoreTrend(0)
                    .streakDays(1)
                    .rank(rank)
                    .byCategory(List.of())
                    .categoryStats(List.of())
                    .weeklyActivity(List.of())
                    .recentProgress(List.of())
                    .weakPoints(List.of())
                    .build();
        }
    }
}
