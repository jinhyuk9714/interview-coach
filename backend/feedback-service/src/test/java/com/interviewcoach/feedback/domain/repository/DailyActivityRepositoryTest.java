package com.interviewcoach.feedback.domain.repository;

import com.interviewcoach.feedback.domain.entity.DailyActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DailyActivityRepository 통합 테스트")
class DailyActivityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DailyActivityRepository dailyActivityRepository;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        entityManager.clear();
    }

    private DailyActivity createAndSaveActivity(Long userId, LocalDate activityDate,
                                                  int questionCount, int totalScore, int interviewCount) {
        DailyActivity activity = DailyActivity.builder()
                .userId(userId)
                .activityDate(activityDate)
                .questionCount(questionCount)
                .totalScore(totalScore)
                .interviewCount(interviewCount)
                .build();
        return entityManager.persist(activity);
    }

    @Nested
    @DisplayName("findByUserIdAndActivityDate - 사용자 ID와 날짜로 조회")
    class FindByUserIdAndActivityDateTest {

        @Test
        @DisplayName("사용자 ID와 날짜로 활동 조회 성공")
        void findByUserIdAndActivityDate_Success() {
            // given
            LocalDate today = LocalDate.of(2025, 2, 1);
            createAndSaveActivity(USER_ID, today, 5, 400, 1);
            entityManager.flush();

            // when
            Optional<DailyActivity> result = dailyActivityRepository
                    .findByUserIdAndActivityDate(USER_ID, today);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getUserId()).isEqualTo(USER_ID);
            assertThat(result.get().getActivityDate()).isEqualTo(today);
            assertThat(result.get().getQuestionCount()).isEqualTo(5);
            assertThat(result.get().getTotalScore()).isEqualTo(400);
            assertThat(result.get().getInterviewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("존재하지 않는 날짜 조회 - empty 반환")
        void findByUserIdAndActivityDate_NotFound() {
            // given
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 2, 1), 5, 400, 1);
            entityManager.flush();

            // when
            Optional<DailyActivity> result = dailyActivityRepository
                    .findByUserIdAndActivityDate(USER_ID, LocalDate.of(2025, 2, 2));

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndActivityDateAfter - 특정 날짜 이후 활동 조회")
    class FindByUserIdAndActivityDateAfterTest {

        @Test
        @DisplayName("특정 날짜 이후 활동 목록 조회 성공 - 오름차순 정렬")
        void findAfterDate_Success() {
            // given
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 1, 28), 3, 240, 1);
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 1, 30), 5, 400, 1);
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 2, 1), 4, 320, 1);
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 2, 3), 6, 510, 2);
            entityManager.flush();

            // when
            List<DailyActivity> activities = dailyActivityRepository
                    .findByUserIdAndActivityDateAfter(USER_ID, LocalDate.of(2025, 1, 29));

            // then
            assertThat(activities).hasSize(3);
            assertThat(activities.get(0).getActivityDate()).isEqualTo(LocalDate.of(2025, 1, 30));
            assertThat(activities.get(1).getActivityDate()).isEqualTo(LocalDate.of(2025, 2, 1));
            assertThat(activities.get(2).getActivityDate()).isEqualTo(LocalDate.of(2025, 2, 3));
        }

        @Test
        @DisplayName("특정 날짜 이후 활동이 없는 경우 빈 목록 반환")
        void findAfterDate_Empty() {
            // given
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 1, 28), 3, 240, 1);
            entityManager.flush();

            // when
            List<DailyActivity> activities = dailyActivityRepository
                    .findByUserIdAndActivityDateAfter(USER_ID, LocalDate.of(2025, 2, 1));

            // then
            assertThat(activities).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndActivityDateBetween - 날짜 범위 활동 조회")
    class FindByUserIdAndActivityDateBetweenTest {

        @Test
        @DisplayName("날짜 범위 내 활동 목록 조회 성공 - 오름차순 정렬")
        void findBetweenDates_Success() {
            // given
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 1, 27), 2, 160, 1);
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 1, 28), 3, 240, 1);
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 1, 30), 5, 400, 1);
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 2, 1), 4, 320, 1);
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 2, 3), 6, 510, 2);
            entityManager.flush();

            // when
            List<DailyActivity> activities = dailyActivityRepository
                    .findByUserIdAndActivityDateBetween(
                            USER_ID,
                            LocalDate.of(2025, 1, 28),
                            LocalDate.of(2025, 2, 1));

            // then
            assertThat(activities).hasSize(3);
            assertThat(activities.get(0).getActivityDate()).isEqualTo(LocalDate.of(2025, 1, 28));
            assertThat(activities.get(1).getActivityDate()).isEqualTo(LocalDate.of(2025, 1, 30));
            assertThat(activities.get(2).getActivityDate()).isEqualTo(LocalDate.of(2025, 2, 1));
        }

        @Test
        @DisplayName("날짜 범위 내 활동이 없는 경우 빈 목록 반환")
        void findBetweenDates_Empty() {
            // given
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 1, 27), 2, 160, 1);
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 2, 5), 6, 510, 2);
            entityManager.flush();

            // when
            List<DailyActivity> activities = dailyActivityRepository
                    .findByUserIdAndActivityDateBetween(
                            USER_ID,
                            LocalDate.of(2025, 1, 28),
                            LocalDate.of(2025, 2, 1));

            // then
            assertThat(activities).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자의 활동은 조회되지 않음")
        void findBetweenDates_OnlyOwnActivities() {
            // given
            Long otherUserId = 999L;
            createAndSaveActivity(USER_ID, LocalDate.of(2025, 1, 30), 5, 400, 1);
            createAndSaveActivity(otherUserId, LocalDate.of(2025, 1, 30), 3, 200, 1);
            entityManager.flush();

            // when
            List<DailyActivity> activities = dailyActivityRepository
                    .findByUserIdAndActivityDateBetween(
                            USER_ID,
                            LocalDate.of(2025, 1, 28),
                            LocalDate.of(2025, 2, 1));

            // then
            assertThat(activities).hasSize(1);
            assertThat(activities.get(0).getUserId()).isEqualTo(USER_ID);
        }
    }
}
