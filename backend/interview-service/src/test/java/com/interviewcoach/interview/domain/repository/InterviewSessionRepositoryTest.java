package com.interviewcoach.interview.domain.repository;

import com.interviewcoach.interview.domain.entity.InterviewQna;
import com.interviewcoach.interview.domain.entity.InterviewSession;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("InterviewSessionRepository 통합 테스트")
class InterviewSessionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InterviewSessionRepository sessionRepository;

    private static final Long USER_ID = 1L;
    private static final Long JD_ID = 10L;

    @BeforeEach
    void setUp() {
        // Clear any existing data
        entityManager.clear();
    }

    @Nested
    @DisplayName("면접 기록 저장 테스트")
    class SaveTest {

        @Test
        @DisplayName("면접 세션 저장 성공")
        void save_Success() {
            // given
            InterviewSession session = InterviewSession.builder()
                    .userId(USER_ID)
                    .jdId(JD_ID)
                    .interviewType("technical")
                    .status("in_progress")
                    .build();

            // when
            InterviewSession saved = sessionRepository.save(session);
            entityManager.flush();

            // then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getStatus()).isEqualTo("in_progress");
        }

        @Test
        @DisplayName("면접 세션과 QnA 함께 저장")
        @Disabled("H2는 jsonb 타입을 지원하지 않음 - PostgreSQL에서만 실행")
        void save_WithQna_Success() {
            // given
            InterviewSession session = InterviewSession.builder()
                    .userId(USER_ID)
                    .jdId(JD_ID)
                    .interviewType("technical")
                    .status("in_progress")
                    .build();

            InterviewQna qna1 = InterviewQna.builder()
                    .session(session)
                    .questionOrder(1)
                    .questionType("technical")
                    .questionText("Java의 GC에 대해 설명해주세요.")
                    .build();

            InterviewQna qna2 = InterviewQna.builder()
                    .session(session)
                    .questionOrder(2)
                    .questionType("behavioral")
                    .questionText("팀 갈등을 해결한 경험이 있나요?")
                    .build();

            session.getQnaList().add(qna1);
            session.getQnaList().add(qna2);

            // when
            InterviewSession saved = sessionRepository.save(session);
            entityManager.flush();
            entityManager.clear();

            // then
            InterviewSession found = sessionRepository.findById(saved.getId()).orElseThrow();
            assertThat(found.getQnaList()).hasSize(2);
            assertThat(found.getQnaList().get(0).getQuestionText()).isEqualTo("Java의 GC에 대해 설명해주세요.");
        }

        @Test
        @DisplayName("완료된 면접 저장 - 평균 점수 포함")
        void save_CompletedWithScore() {
            // given
            InterviewSession session = InterviewSession.builder()
                    .userId(USER_ID)
                    .jdId(JD_ID)
                    .interviewType("technical")
                    .status("completed")
                    .avgScore(BigDecimal.valueOf(85))
                    .build();

            // when
            InterviewSession saved = sessionRepository.save(session);
            entityManager.flush();
            entityManager.clear();

            // then
            InterviewSession found = sessionRepository.findById(saved.getId()).orElseThrow();
            assertThat(found.getStatus()).isEqualTo("completed");
            assertThat(found.getAvgScore()).isEqualByComparingTo(BigDecimal.valueOf(85));
        }
    }

    @Nested
    @DisplayName("면접 기록 조회 테스트")
    class FindTest {

        @Test
        @DisplayName("사용자 ID로 면접 목록 조회 - 최신순 정렬")
        void findByUserIdOrderByStartedAtDesc() {
            // given
            InterviewSession session1 = createAndSaveSession(USER_ID, "completed", LocalDateTime.now().minusDays(2));
            InterviewSession session2 = createAndSaveSession(USER_ID, "completed", LocalDateTime.now().minusDays(1));
            InterviewSession session3 = createAndSaveSession(USER_ID, "in_progress", LocalDateTime.now());
            entityManager.flush();

            // when
            List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(USER_ID);

            // then
            assertThat(sessions).hasSize(3);
            assertThat(sessions.get(0).getId()).isEqualTo(session3.getId()); // 최신
            assertThat(sessions.get(1).getId()).isEqualTo(session2.getId());
            assertThat(sessions.get(2).getId()).isEqualTo(session1.getId()); // 오래됨
        }

        @Test
        @DisplayName("사용자 ID와 상태로 면접 목록 조회")
        void findByUserIdAndStatus() {
            // given
            createAndSaveSession(USER_ID, "completed", LocalDateTime.now().minusDays(2));
            createAndSaveSession(USER_ID, "completed", LocalDateTime.now().minusDays(1));
            createAndSaveSession(USER_ID, "in_progress", LocalDateTime.now());
            entityManager.flush();

            // when
            List<InterviewSession> completedSessions = sessionRepository.findByUserIdAndStatus(USER_ID, "completed");

            // then
            assertThat(completedSessions).hasSize(2);
            assertThat(completedSessions).allMatch(s -> s.getStatus().equals("completed"));
        }

        @Test
        @DisplayName("다른 사용자의 면접은 조회되지 않음")
        void findByUserId_OnlyOwnSessions() {
            // given
            Long otherUserId = 999L;
            createAndSaveSession(USER_ID, "completed", LocalDateTime.now());
            createAndSaveSession(otherUserId, "completed", LocalDateTime.now());
            entityManager.flush();

            // when
            List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(USER_ID);

            // then
            assertThat(sessions).hasSize(1);
            assertThat(sessions.get(0).getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("면접 기록이 없는 경우 빈 목록 반환")
        void findByUserId_Empty() {
            // given - 데이터 없음

            // when
            List<InterviewSession> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(USER_ID);

            // then
            assertThat(sessions).isEmpty();
        }
    }

    @Nested
    @DisplayName("QnA 피드백 저장 테스트")
    @Disabled("H2는 jsonb 타입을 지원하지 않음 - PostgreSQL에서만 실행")
    class QnaFeedbackTest {

        @Test
        @DisplayName("QnA에 답변과 피드백 저장")
        void saveQnaWithFeedback() {
            // given
            InterviewSession session = InterviewSession.builder()
                    .userId(USER_ID)
                    .jdId(JD_ID)
                    .interviewType("technical")
                    .status("in_progress")
                    .build();

            InterviewQna qna = InterviewQna.builder()
                    .session(session)
                    .questionOrder(1)
                    .questionType("technical")
                    .questionText("Spring DI에 대해 설명해주세요.")
                    .build();

            session.getQnaList().add(qna);
            sessionRepository.save(session);
            entityManager.flush();

            // when - 답변 제출
            qna.submitAnswer("Spring DI는 의존성 주입으로, 객체 간의 의존 관계를 외부에서 주입받는 방식입니다.");
            qna.setFeedback(Map.of(
                    "score", 85,
                    "strengths", List.of("핵심 개념을 잘 설명했습니다"),
                    "improvements", List.of("실제 사용 예시를 추가하면 좋겠습니다")
            ));
            sessionRepository.save(session);
            entityManager.flush();
            entityManager.clear();

            // then
            InterviewSession found = sessionRepository.findById(session.getId()).orElseThrow();
            InterviewQna foundQna = found.getQnaList().get(0);

            assertThat(foundQna.getAnswerText()).contains("Spring DI");
            assertThat(foundQna.getAnsweredAt()).isNotNull();
            assertThat(foundQna.getFeedback()).isNotNull();
            assertThat(foundQna.getFeedback().get("score")).isEqualTo(85);
        }

        @Test
        @DisplayName("면접 완료 시 평균 점수 계산 후 저장")
        void completeInterviewWithAvgScore() {
            // given
            InterviewSession session = InterviewSession.builder()
                    .userId(USER_ID)
                    .jdId(JD_ID)
                    .interviewType("technical")
                    .status("in_progress")
                    .build();

            // QnA 추가 및 피드백 설정
            int[] scores = {70, 80, 90};
            for (int i = 0; i < scores.length; i++) {
                InterviewQna qna = InterviewQna.builder()
                        .session(session)
                        .questionOrder(i + 1)
                        .questionType("technical")
                        .questionText("질문 " + (i + 1))
                        .build();
                qna.submitAnswer("답변 " + (i + 1));
                qna.setFeedback(Map.of("score", scores[i], "strengths", List.of("좋음"), "improvements", List.of("개선점")));
                session.getQnaList().add(qna);
            }

            sessionRepository.save(session);
            entityManager.flush();

            // when - 면접 완료
            session.complete(BigDecimal.valueOf(80));
            sessionRepository.save(session);
            entityManager.flush();
            entityManager.clear();

            // then
            InterviewSession found = sessionRepository.findById(session.getId()).orElseThrow();
            assertThat(found.getStatus()).isEqualTo("completed");
            assertThat(found.getAvgScore()).isEqualByComparingTo(BigDecimal.valueOf(80));
            assertThat(found.getCompletedAt()).isNotNull();
        }
    }

    // Helper method
    private InterviewSession createAndSaveSession(Long userId, String status, LocalDateTime startedAt) {
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .jdId(JD_ID)
                .interviewType("technical")
                .status(status)
                .build();

        // Use reflection to set startedAt since it's set automatically
        try {
            java.lang.reflect.Field field = InterviewSession.class.getDeclaredField("startedAt");
            field.setAccessible(true);
            field.set(session, startedAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if ("completed".equals(status)) {
            session.complete(BigDecimal.valueOf(80));
        }

        return entityManager.persist(session);
    }
}
