package com.interviewcoach.feedback.application.service;

import com.interviewcoach.feedback.application.dto.request.RecordAnswerRequest;
import com.interviewcoach.feedback.application.dto.response.StatisticsResponse;
import com.interviewcoach.feedback.application.dto.response.UserStatisticsSummaryResponse;
import com.interviewcoach.feedback.domain.entity.DailyActivity;
import com.interviewcoach.feedback.domain.entity.UserStatistics;
import com.interviewcoach.feedback.domain.repository.DailyActivityRepository;
import com.interviewcoach.feedback.domain.repository.UserStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsService 단위 테스트")
class StatisticsServiceTest {

    @Mock
    private UserStatisticsRepository statisticsRepository;

    @Mock
    private DailyActivityRepository dailyActivityRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("recordAnswer 메서드")
    class RecordAnswerTest {

        @BeforeEach
        void setUpDailyActivity() {
            DailyActivity dailyActivity = DailyActivity.builder()
                    .userId(USER_ID)
                    .activityDate(LocalDate.now())
                    .build();
            given(dailyActivityRepository.findByUserIdAndActivityDateWithLock(eq(USER_ID), any(LocalDate.class)))
                    .willReturn(Optional.of(dailyActivity));
        }

        @Test
        @DisplayName("새로운 카테고리의 첫 답변 기록")
        void recordAnswer_NewCategory() throws Exception {
            // given
            RecordAnswerRequest request = createRecordRequest("Java", true, null);

            UserStatistics newStats = UserStatistics.builder()
                    .userId(USER_ID)
                    .skillCategory("Java")
                    .build();

            UserStatistics savedStats = createUserStatistics(1L, USER_ID, "Java", 1, 1, BigDecimal.valueOf(100.00));

            given(statisticsRepository.findByUserIdAndSkillCategoryWithLock(USER_ID, "Java"))
                    .willReturn(Optional.empty());
            given(statisticsRepository.save(any(UserStatistics.class))).willReturn(newStats);

            // when
            StatisticsResponse response = statisticsService.recordAnswer(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getSkillCategory()).isEqualTo("Java");
            verify(statisticsRepository, times(1)).save(any(UserStatistics.class));
        }

        @Test
        @DisplayName("기존 카테고리에 정답 기록")
        void recordAnswer_ExistingCategory_Correct() throws Exception {
            // given
            RecordAnswerRequest request = createRecordRequest("Spring", true, null);

            // 기존: 5문제, 총점 300 (평균 60점)
            UserStatistics existingStats = createUserStatisticsWithTotalScore(
                    1L, USER_ID, "Spring", 5, 3, 300, BigDecimal.valueOf(60.00));

            given(statisticsRepository.findByUserIdAndSkillCategoryWithLock(USER_ID, "Spring"))
                    .willReturn(Optional.of(existingStats));

            // when - 정답(score 없음 -> 100점으로 처리)
            StatisticsResponse response = statisticsService.recordAnswer(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            // totalScore: 300 + 100 = 400, totalQuestions: 6, correctCount: 4
            assertThat(existingStats.getTotalQuestions()).isEqualTo(6);
            assertThat(existingStats.getCorrectCount()).isEqualTo(4);
            assertThat(existingStats.getTotalScore()).isEqualTo(400);
        }

        @Test
        @DisplayName("오답 기록 시 평균 점수 감소")
        void recordAnswer_Incorrect() throws Exception {
            // given
            RecordAnswerRequest request = createRecordRequest("Database", false, "SQL 최적화");

            // 기존: 4문제, 총점 400 (평균 100점)
            UserStatistics existingStats = createUserStatisticsWithTotalScore(
                    1L, USER_ID, "Database", 4, 4, 400, BigDecimal.valueOf(100.00));

            given(statisticsRepository.findByUserIdAndSkillCategoryWithLock(USER_ID, "Database"))
                    .willReturn(Optional.of(existingStats));

            // when - 오답(score 없음 -> 0점으로 처리)
            statisticsService.recordAnswer(USER_ID, request);

            // then
            // totalScore: 400 + 0 = 400, totalQuestions: 5, correctRate: 400/5 = 80.00
            assertThat(existingStats.getTotalQuestions()).isEqualTo(5);
            assertThat(existingStats.getCorrectCount()).isEqualTo(4);
            assertThat(existingStats.getTotalScore()).isEqualTo(400);
            assertThat(existingStats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
        }

        @Test
        @DisplayName("취약점 추가")
        void recordAnswer_WithWeakPoint() throws Exception {
            // given
            RecordAnswerRequest request = createRecordRequest("Algorithm", false, "재귀 문제");

            UserStatistics existingStats = createUserStatisticsWithTotalScore(
                    1L, USER_ID, "Algorithm", 3, 2, 200, BigDecimal.valueOf(66.67));

            given(statisticsRepository.findByUserIdAndSkillCategoryWithLock(USER_ID, "Algorithm"))
                    .willReturn(Optional.of(existingStats));

            // when
            statisticsService.recordAnswer(USER_ID, request);

            // then
            assertThat(existingStats.getWeakPoints()).contains("재귀 문제");
        }

        @Test
        @DisplayName("실제 점수로 답변 기록 - 75점")
        void recordAnswer_WithActualScore() throws Exception {
            // given
            RecordAnswerRequest request = createRecordRequestWithScore("Java", true, null, 75);

            UserStatistics existingStats = createUserStatisticsWithTotalScore(
                    1L, USER_ID, "Java", 4, 3, 300, BigDecimal.valueOf(75.00));

            given(statisticsRepository.findByUserIdAndSkillCategoryWithLock(USER_ID, "Java"))
                    .willReturn(Optional.of(existingStats));

            // when
            statisticsService.recordAnswer(USER_ID, request);

            // then
            // totalQuestions: 4 -> 5, totalScore: 300 -> 375, correctRate: 375/5 = 75.00
            assertThat(existingStats.getTotalQuestions()).isEqualTo(5);
            assertThat(existingStats.getTotalScore()).isEqualTo(375);
            assertThat(existingStats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(75.00));
        }

        @Test
        @DisplayName("점수 없이 정답 기록 시 100점으로 처리")
        void recordAnswer_NoScore_Correct_Uses100() throws Exception {
            // given
            RecordAnswerRequest request = createRecordRequest("Spring", true, null);

            UserStatistics existingStats = createUserStatisticsWithTotalScore(
                    1L, USER_ID, "Spring", 2, 2, 200, BigDecimal.valueOf(100.00));

            given(statisticsRepository.findByUserIdAndSkillCategoryWithLock(USER_ID, "Spring"))
                    .willReturn(Optional.of(existingStats));

            // when
            statisticsService.recordAnswer(USER_ID, request);

            // then
            // score가 null이고 isCorrect가 true면 100점으로 처리
            // totalScore: 200 -> 300, totalQuestions: 2 -> 3, correctRate: 300/3 = 100.00
            assertThat(existingStats.getTotalScore()).isEqualTo(300);
            assertThat(existingStats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(100.00));
        }

        @Test
        @DisplayName("점수 없이 오답 기록 시 0점으로 처리")
        void recordAnswer_NoScore_Incorrect_Uses0() throws Exception {
            // given
            RecordAnswerRequest request = createRecordRequest("Database", false, null);

            UserStatistics existingStats = createUserStatisticsWithTotalScore(
                    1L, USER_ID, "Database", 2, 2, 200, BigDecimal.valueOf(100.00));

            given(statisticsRepository.findByUserIdAndSkillCategoryWithLock(USER_ID, "Database"))
                    .willReturn(Optional.of(existingStats));

            // when
            statisticsService.recordAnswer(USER_ID, request);

            // then
            // score가 null이고 isCorrect가 false면 0점으로 처리
            // totalScore: 200 -> 200, totalQuestions: 2 -> 3, correctRate: 200/3 = 66.67
            assertThat(existingStats.getTotalScore()).isEqualTo(200);
            assertThat(existingStats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(66.67));
        }

        @Test
        @DisplayName("다양한 점수로 평균 점수 계산")
        void recordAnswer_MultipleScores_CalculatesAverage() throws Exception {
            // given - 기존: 3문제, 총점 210 (평균 70)
            UserStatistics existingStats = createUserStatisticsWithTotalScore(
                    1L, USER_ID, "Java", 3, 2, 210, BigDecimal.valueOf(70.00));

            given(statisticsRepository.findByUserIdAndSkillCategoryWithLock(USER_ID, "Java"))
                    .willReturn(Optional.of(existingStats));

            // when - 85점 추가
            RecordAnswerRequest request = createRecordRequestWithScore("Java", true, null, 85);
            statisticsService.recordAnswer(USER_ID, request);

            // then
            // totalScore: 210 + 85 = 295, totalQuestions: 4, correctRate: 295/4 = 73.75
            assertThat(existingStats.getTotalQuestions()).isEqualTo(4);
            assertThat(existingStats.getTotalScore()).isEqualTo(295);
            assertThat(existingStats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(73.75));
        }
    }

    @Nested
    @DisplayName("getStatistics 메서드")
    class GetStatisticsTest {

        @Test
        @DisplayName("사용자 통계 요약 조회")
        void getStatistics_Success() throws Exception {
            // given - totalScore 기반 평균 계산
            List<UserStatistics> statsList = List.of(
                    createUserStatisticsWithTotalScore(1L, USER_ID, "Java", 10, 8, 800, BigDecimal.valueOf(80.00)),
                    createUserStatisticsWithTotalScore(2L, USER_ID, "Spring", 8, 6, 600, BigDecimal.valueOf(75.00)),
                    createUserStatisticsWithTotalScore(3L, USER_ID, "Database", 5, 2, 200, BigDecimal.valueOf(40.00))
            );

            given(statisticsRepository.findByUserIdOrderByCorrectRateDesc(USER_ID))
                    .willReturn(statsList);

            // when
            UserStatisticsSummaryResponse response = statisticsService.getStatistics(USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getTotalQuestions()).isEqualTo(23); // 10 + 8 + 5
            assertThat(response.getTotalCorrect()).isEqualTo(16); // 8 + 6 + 2
            assertThat(response.getByCategory()).hasSize(3);
        }

        @Test
        @DisplayName("통계가 없는 사용자 조회")
        void getStatistics_Empty() {
            // given
            given(statisticsRepository.findByUserIdOrderByCorrectRateDesc(USER_ID))
                    .willReturn(List.of());

            // when
            UserStatisticsSummaryResponse response = statisticsService.getStatistics(USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getTotalQuestions()).isEqualTo(0);
            assertThat(response.getTotalCorrect()).isEqualTo(0);
            assertThat(response.getOverallCorrectRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("전체 정답률 계산 검증 - totalScore 기반")
        void getStatistics_OverallCorrectRate() throws Exception {
            // given - totalScore 기반으로 평균 점수 계산
            // Java: 10문제, 총점 1000 (평균 100)
            // Spring: 10문제, 총점 0 (평균 0)
            // 전체: 20문제, 총점 1000, 평균 50
            List<UserStatistics> statsList = List.of(
                    createUserStatisticsWithTotalScore(1L, USER_ID, "Java", 10, 10, 1000, BigDecimal.valueOf(100.00)),
                    createUserStatisticsWithTotalScore(2L, USER_ID, "Spring", 10, 0, 0, BigDecimal.valueOf(0.00))
            );

            given(statisticsRepository.findByUserIdOrderByCorrectRateDesc(USER_ID))
                    .willReturn(statsList);

            // when
            UserStatisticsSummaryResponse response = statisticsService.getStatistics(USER_ID);

            // then
            // 전체 평균: totalScore / totalQuestions = 1000 / 20 = 50
            assertThat(response.getOverallCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        }

        @Test
        @DisplayName("랭크 계산 검증")
        void getStatistics_RankCalculation() throws Exception {
            // given - 90점 이상 S랭크 (totalScore 기반으로 계산됨)
            // 10문제, 총점 900, 평균 90점 -> S랭크
            List<UserStatistics> statsList = List.of(
                    createUserStatisticsWithTotalScore(1L, USER_ID, "Java", 10, 9, 900, BigDecimal.valueOf(90.00))
            );

            given(statisticsRepository.findByUserIdOrderByCorrectRateDesc(USER_ID))
                    .willReturn(statsList);

            // when
            UserStatisticsSummaryResponse response = statisticsService.getStatistics(USER_ID);

            // then
            assertThat(response.getRank()).isEqualTo("S");
        }

        @Test
        @DisplayName("취약점 응답 생성 검증")
        void getStatistics_WeakPoints() throws Exception {
            // given - 70점 미만은 취약점으로 표시
            List<UserStatistics> statsList = List.of(
                    createUserStatisticsWithTotalScore(1L, USER_ID, "Java", 10, 9, 900, BigDecimal.valueOf(90.00)),
                    createUserStatisticsWithTotalScore(2L, USER_ID, "Database", 10, 5, 500, BigDecimal.valueOf(50.00))
            );

            given(statisticsRepository.findByUserIdOrderByCorrectRateDesc(USER_ID))
                    .willReturn(statsList);

            // when
            UserStatisticsSummaryResponse response = statisticsService.getStatistics(USER_ID);

            // then
            assertThat(response.getWeakPoints()).hasSize(1);
            assertThat(response.getWeakPoints().get(0).getSkill()).isEqualTo("Database");
        }

        @Test
        @DisplayName("평균 점수가 실제 점수 기반으로 계산됨")
        void getStatistics_AvgScoreFromTotalScore() throws Exception {
            // given - 여러 카테고리의 실제 점수를 기반으로 평균 계산
            // Java: 10문제, 총점 750 (평균 75)
            // Spring: 5문제, 총점 400 (평균 80)
            // 전체: 15문제, 총점 1150, 평균 = 1150/15 = 76.67
            List<UserStatistics> statsList = List.of(
                    createUserStatisticsWithTotalScore(1L, USER_ID, "Java", 10, 8, 750, BigDecimal.valueOf(75.00)),
                    createUserStatisticsWithTotalScore(2L, USER_ID, "Spring", 5, 4, 400, BigDecimal.valueOf(80.00))
            );

            given(statisticsRepository.findByUserIdOrderByCorrectRateDesc(USER_ID))
                    .willReturn(statsList);

            // when
            UserStatisticsSummaryResponse response = statisticsService.getStatistics(USER_ID);

            // then
            assertThat(response.getTotalQuestions()).isEqualTo(15);
            // avgScore = totalScore / totalQuestions = 1150 / 15 = 76.67 -> 76
            assertThat(response.getAvgScore()).isEqualTo(76);
            assertThat(response.getRank()).isEqualTo("B"); // 70-80은 B랭크
        }

        @Test
        @DisplayName("전체 정답률이 실제 점수 기반으로 계산됨")
        void getStatistics_OverallRateFromTotalScore() throws Exception {
            // given - 다양한 점수의 카테고리
            List<UserStatistics> statsList = List.of(
                    createUserStatisticsWithTotalScore(1L, USER_ID, "Java", 4, 3, 300, BigDecimal.valueOf(75.00)),
                    createUserStatisticsWithTotalScore(2L, USER_ID, "Spring", 4, 2, 260, BigDecimal.valueOf(65.00)),
                    createUserStatisticsWithTotalScore(3L, USER_ID, "Database", 2, 2, 180, BigDecimal.valueOf(90.00))
            );

            given(statisticsRepository.findByUserIdOrderByCorrectRateDesc(USER_ID))
                    .willReturn(statsList);

            // when
            UserStatisticsSummaryResponse response = statisticsService.getStatistics(USER_ID);

            // then
            // totalScore = 300 + 260 + 180 = 740
            // totalQuestions = 4 + 4 + 2 = 10
            // avgScore = 740 / 10 = 74
            assertThat(response.getAvgScore()).isEqualTo(74);
            assertThat(response.getOverallCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(74.00));
        }
    }

    @Nested
    @DisplayName("getStatisticsByCategory 메서드")
    class GetStatisticsByCategoryTest {

        @Test
        @DisplayName("특정 카테고리 통계 조회 성공")
        void getStatisticsByCategory_Success() throws Exception {
            // given
            UserStatistics stats = createUserStatisticsWithTotalScore(1L, USER_ID, "Java", 10, 8, 800, BigDecimal.valueOf(80.00));

            given(statisticsRepository.findByUserIdAndSkillCategory(USER_ID, "Java"))
                    .willReturn(Optional.of(stats));

            // when
            StatisticsResponse response = statisticsService.getStatisticsByCategory(USER_ID, "Java");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getSkillCategory()).isEqualTo("Java");
            assertThat(response.getTotalQuestions()).isEqualTo(10);
            assertThat(response.getCorrectCount()).isEqualTo(8);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 조회 시 기본값 반환")
        void getStatisticsByCategory_NotFound() {
            // given
            given(statisticsRepository.findByUserIdAndSkillCategory(USER_ID, "Unknown"))
                    .willReturn(Optional.empty());

            // when
            StatisticsResponse response = statisticsService.getStatisticsByCategory(USER_ID, "Unknown");

            // then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getSkillCategory()).isEqualTo("Unknown");
            assertThat(response.getTotalQuestions()).isEqualTo(0);
        }
    }

    // Helper methods
    private RecordAnswerRequest createRecordRequest(String skillCategory, Boolean isCorrect, String weakPoint) throws Exception {
        RecordAnswerRequest request = new RecordAnswerRequest();
        setField(request, "skillCategory", skillCategory);
        setField(request, "isCorrect", isCorrect);
        setField(request, "weakPoint", weakPoint);
        return request;
    }

    private RecordAnswerRequest createRecordRequestWithScore(String skillCategory, Boolean isCorrect, String weakPoint, Integer score) throws Exception {
        RecordAnswerRequest request = new RecordAnswerRequest();
        setField(request, "skillCategory", skillCategory);
        setField(request, "isCorrect", isCorrect);
        setField(request, "weakPoint", weakPoint);
        setField(request, "score", score);
        return request;
    }

    private UserStatistics createUserStatistics(Long id, Long userId, String skillCategory,
                                                 int totalQuestions, int correctCount, BigDecimal correctRate) throws Exception {
        UserStatistics stats = UserStatistics.builder()
                .userId(userId)
                .skillCategory(skillCategory)
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .correctRate(correctRate)
                .build();
        setField(stats, "id", id);
        return stats;
    }

    private UserStatistics createUserStatisticsWithTotalScore(Long id, Long userId, String skillCategory,
                                                               int totalQuestions, int correctCount, int totalScore, BigDecimal correctRate) throws Exception {
        UserStatistics stats = UserStatistics.builder()
                .userId(userId)
                .skillCategory(skillCategory)
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .totalScore(totalScore)
                .correctRate(correctRate)
                .build();
        setField(stats, "id", id);
        return stats;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
