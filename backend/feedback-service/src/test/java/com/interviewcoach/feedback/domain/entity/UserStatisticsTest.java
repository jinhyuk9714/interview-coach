package com.interviewcoach.feedback.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserStatistics 엔티티 단위 테스트")
class UserStatisticsTest {

    @Nested
    @DisplayName("recordAnswer 메서드")
    class RecordAnswerTest {

        @Test
        @DisplayName("첫 번째 정답 기록 - 100점")
        void recordAnswer_FirstCorrectAnswer() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Java")
                    .build();

            // when
            stats.recordAnswer(true);

            // then
            assertThat(stats.getTotalQuestions()).isEqualTo(1);
            assertThat(stats.getCorrectCount()).isEqualTo(1);
            assertThat(stats.getTotalScore()).isEqualTo(100);
            assertThat(stats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        @DisplayName("첫 번째 오답 기록 - 0점")
        void recordAnswer_FirstIncorrectAnswer() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Java")
                    .build();

            // when
            stats.recordAnswer(false);

            // then
            assertThat(stats.getTotalQuestions()).isEqualTo(1);
            assertThat(stats.getCorrectCount()).isEqualTo(0);
            assertThat(stats.getTotalScore()).isEqualTo(0);
            assertThat(stats.getCorrectRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("실제 점수로 기록 - 75점")
        void recordAnswer_WithActualScore() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Java")
                    .build();

            // when
            stats.recordAnswer(true, 75);

            // then
            assertThat(stats.getTotalQuestions()).isEqualTo(1);
            assertThat(stats.getCorrectCount()).isEqualTo(1);
            assertThat(stats.getTotalScore()).isEqualTo(75);
            assertThat(stats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(75));
        }

        @Test
        @DisplayName("여러 답변 후 평균 점수 계산")
        void recordAnswer_MultipleAnswers_CalculatesAverage() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Java")
                    .build();

            // when - 80점, 70점, 90점 기록
            stats.recordAnswer(true, 80);
            stats.recordAnswer(true, 70);
            stats.recordAnswer(true, 90);

            // then - 평균: (80 + 70 + 90) / 3 = 80
            assertThat(stats.getTotalQuestions()).isEqualTo(3);
            assertThat(stats.getCorrectCount()).isEqualTo(3);
            assertThat(stats.getTotalScore()).isEqualTo(240);
            assertThat(stats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(80));
        }

        @Test
        @DisplayName("정답과 오답 혼합 - 평균 점수 계산")
        void recordAnswer_MixedResults() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Spring")
                    .build();

            // when - 85점(정답), 45점(오답), 75점(정답), 65점(정답)
            stats.recordAnswer(true, 85);
            stats.recordAnswer(false, 45);
            stats.recordAnswer(true, 75);
            stats.recordAnswer(true, 65);

            // then
            assertThat(stats.getTotalQuestions()).isEqualTo(4);
            assertThat(stats.getCorrectCount()).isEqualTo(3);
            assertThat(stats.getTotalScore()).isEqualTo(270); // 85 + 45 + 75 + 65
            // 평균: 270 / 4 = 67.5
            assertThat(stats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(67.5));
        }

        @Test
        @DisplayName("0점 기록")
        void recordAnswer_ZeroScore() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Database")
                    .totalQuestions(2)
                    .correctCount(2)
                    .totalScore(180)
                    .correctRate(BigDecimal.valueOf(90))
                    .build();

            // when - 0점 추가
            stats.recordAnswer(false, 0);

            // then
            assertThat(stats.getTotalQuestions()).isEqualTo(3);
            assertThat(stats.getCorrectCount()).isEqualTo(2);
            assertThat(stats.getTotalScore()).isEqualTo(180);
            // 평균: 180 / 3 = 60
            assertThat(stats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(60));
        }

        @Test
        @DisplayName("소수점 평균 점수 - 반올림")
        void recordAnswer_DecimalAverage_RoundsHalfUp() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Algorithm")
                    .build();

            // when - 77점, 78점, 79점 기록 (평균: 234/3 = 78.00)
            stats.recordAnswer(true, 77);
            stats.recordAnswer(true, 78);
            stats.recordAnswer(true, 79);

            // then
            assertThat(stats.getTotalScore()).isEqualTo(234);
            assertThat(stats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(78));
        }

        @Test
        @DisplayName("소수점 반올림 검증 - 66.67")
        void recordAnswer_DecimalAverage_RoundsCorrectly() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Java")
                    .build();

            // when - 100점, 100점, 0점 기록 (평균: 200/3 = 66.67)
            stats.recordAnswer(true, 100);
            stats.recordAnswer(true, 100);
            stats.recordAnswer(false, 0);

            // then
            assertThat(stats.getTotalScore()).isEqualTo(200);
            assertThat(stats.getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(66.67));
        }
    }

    @Nested
    @DisplayName("addWeakPoint 메서드")
    class AddWeakPointTest {

        @Test
        @DisplayName("취약점 추가")
        void addWeakPoint_Success() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Java")
                    .build();

            // when
            stats.addWeakPoint("상속 개념");

            // then
            assertThat(stats.getWeakPoints()).contains("상속 개념");
        }

        @Test
        @DisplayName("중복 취약점은 추가되지 않음")
        void addWeakPoint_NoDuplicate() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Java")
                    .build();

            // when
            stats.addWeakPoint("상속 개념");
            stats.addWeakPoint("상속 개념");
            stats.addWeakPoint("다형성");

            // then
            assertThat(stats.getWeakPoints()).hasSize(2);
            assertThat(stats.getWeakPoints()).containsExactly("상속 개념", "다형성");
        }

        @Test
        @DisplayName("여러 취약점 추가")
        void addWeakPoint_Multiple() {
            // given
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Spring")
                    .build();

            // when
            stats.addWeakPoint("DI 이해");
            stats.addWeakPoint("AOP 적용");
            stats.addWeakPoint("트랜잭션 관리");

            // then
            assertThat(stats.getWeakPoints()).hasSize(3);
            assertThat(stats.getWeakPoints()).containsExactlyInAnyOrder("DI 이해", "AOP 적용", "트랜잭션 관리");
        }
    }

    @Nested
    @DisplayName("빌더 기본값 테스트")
    class BuilderDefaultsTest {

        @Test
        @DisplayName("빌더 기본값 검증")
        void builder_DefaultValues() {
            // given & when
            UserStatistics stats = UserStatistics.builder()
                    .userId(1L)
                    .skillCategory("Java")
                    .build();

            // then
            assertThat(stats.getTotalQuestions()).isEqualTo(0);
            assertThat(stats.getCorrectCount()).isEqualTo(0);
            assertThat(stats.getTotalScore()).isEqualTo(0);
            assertThat(stats.getCorrectRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(stats.getWeakPoints()).isEmpty();
        }
    }
}
