package com.interviewcoach.feedback.domain.repository;

import com.interviewcoach.feedback.domain.entity.UserStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Disabled("H2는 jsonb 타입을 지원하지 않음 - PostgreSQL 통합 테스트 환경에서 실행")
@DisplayName("UserStatisticsRepository 통합 테스트")
class UserStatisticsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserStatisticsRepository userStatisticsRepository;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        entityManager.clear();
    }

    private UserStatistics createAndSaveStatistics(Long userId, String skillCategory,
                                                    int totalQuestions, int correctCount,
                                                    int totalScore, BigDecimal correctRate) {
        UserStatistics stats = UserStatistics.builder()
                .userId(userId)
                .skillCategory(skillCategory)
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .totalScore(totalScore)
                .correctRate(correctRate)
                .build();
        return entityManager.persist(stats);
    }

    @Nested
    @DisplayName("findByUserId - 사용자 ID로 통계 조회")
    class FindByUserIdTest {

        @Test
        @DisplayName("사용자 ID로 모든 통계 조회 성공")
        void findByUserId_ReturnsAllStats() {
            // given
            createAndSaveStatistics(USER_ID, "Java", 10, 8, 800, BigDecimal.valueOf(80.00));
            createAndSaveStatistics(USER_ID, "Spring", 5, 4, 400, BigDecimal.valueOf(80.00));
            createAndSaveStatistics(USER_ID, "Database", 3, 1, 150, BigDecimal.valueOf(50.00));
            entityManager.flush();

            // when
            List<UserStatistics> stats = userStatisticsRepository.findByUserId(USER_ID);

            // then
            assertThat(stats).hasSize(3);
            assertThat(stats).extracting(UserStatistics::getSkillCategory)
                    .containsExactlyInAnyOrder("Java", "Spring", "Database");
        }

        @Test
        @DisplayName("다른 사용자의 통계는 조회되지 않음")
        void findByUserId_OnlyOwnStats() {
            // given
            Long otherUserId = 999L;
            createAndSaveStatistics(USER_ID, "Java", 10, 8, 800, BigDecimal.valueOf(80.00));
            createAndSaveStatistics(otherUserId, "Java", 5, 2, 200, BigDecimal.valueOf(40.00));
            entityManager.flush();

            // when
            List<UserStatistics> stats = userStatisticsRepository.findByUserId(USER_ID);

            // then
            assertThat(stats).hasSize(1);
            assertThat(stats.get(0).getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("통계가 없는 경우 빈 목록 반환")
        void findByUserId_Empty() {
            // when
            List<UserStatistics> stats = userStatisticsRepository.findByUserId(USER_ID);

            // then
            assertThat(stats).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndSkillCategory - 사용자 ID와 카테고리로 조회")
    class FindByUserIdAndSkillCategoryTest {

        @Test
        @DisplayName("사용자 ID와 카테고리로 통계 조회 성공")
        void findByUserIdAndSkillCategory_Success() {
            // given
            createAndSaveStatistics(USER_ID, "Java", 10, 8, 800, BigDecimal.valueOf(80.00));
            createAndSaveStatistics(USER_ID, "Spring", 5, 4, 400, BigDecimal.valueOf(80.00));
            entityManager.flush();

            // when
            Optional<UserStatistics> result = userStatisticsRepository
                    .findByUserIdAndSkillCategory(USER_ID, "Java");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getSkillCategory()).isEqualTo("Java");
            assertThat(result.get().getTotalQuestions()).isEqualTo(10);
            assertThat(result.get().getCorrectCount()).isEqualTo(8);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 조회 - empty 반환")
        void findByUserIdAndSkillCategory_NotFound() {
            // given
            createAndSaveStatistics(USER_ID, "Java", 10, 8, 800, BigDecimal.valueOf(80.00));
            entityManager.flush();

            // when
            Optional<UserStatistics> result = userStatisticsRepository
                    .findByUserIdAndSkillCategory(USER_ID, "Kotlin");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndSkillCategoryWithLock - 비관적 락 조회")
    class FindWithLockTest {

        @Test
        @DisplayName("비관적 락으로 통계 조회 성공")
        void findWithLock_Success() {
            // given
            createAndSaveStatistics(USER_ID, "Java", 10, 8, 800, BigDecimal.valueOf(80.00));
            entityManager.flush();

            // when
            Optional<UserStatistics> result = userStatisticsRepository
                    .findByUserIdAndSkillCategoryWithLock(USER_ID, "Java");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getSkillCategory()).isEqualTo("Java");
            assertThat(result.get().getTotalQuestions()).isEqualTo(10);
        }

        @Test
        @DisplayName("비관적 락 - 존재하지 않는 데이터 조회 시 empty 반환")
        void findWithLock_NotFound() {
            // when
            Optional<UserStatistics> result = userStatisticsRepository
                    .findByUserIdAndSkillCategoryWithLock(USER_ID, "NonExistent");

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdOrderByCorrectRateDesc - 정답률 내림차순 조회")
    class FindByUserIdOrderByCorrectRateDescTest {

        @Test
        @DisplayName("정답률 내림차순으로 통계 조회 성공")
        void findOrderedByCorrectRate_Success() {
            // given
            createAndSaveStatistics(USER_ID, "Database", 3, 1, 150, BigDecimal.valueOf(50.00));
            createAndSaveStatistics(USER_ID, "Java", 10, 8, 800, BigDecimal.valueOf(80.00));
            createAndSaveStatistics(USER_ID, "Spring", 5, 5, 450, BigDecimal.valueOf(90.00));
            entityManager.flush();

            // when
            List<UserStatistics> stats = userStatisticsRepository
                    .findByUserIdOrderByCorrectRateDesc(USER_ID);

            // then
            assertThat(stats).hasSize(3);
            assertThat(stats.get(0).getSkillCategory()).isEqualTo("Spring");
            assertThat(stats.get(0).getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(90.00));
            assertThat(stats.get(1).getSkillCategory()).isEqualTo("Java");
            assertThat(stats.get(1).getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
            assertThat(stats.get(2).getSkillCategory()).isEqualTo("Database");
            assertThat(stats.get(2).getCorrectRate()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        }

        @Test
        @DisplayName("통계가 없는 경우 빈 목록 반환")
        void findOrderedByCorrectRate_Empty() {
            // when
            List<UserStatistics> stats = userStatisticsRepository
                    .findByUserIdOrderByCorrectRateDesc(USER_ID);

            // then
            assertThat(stats).isEmpty();
        }
    }
}
