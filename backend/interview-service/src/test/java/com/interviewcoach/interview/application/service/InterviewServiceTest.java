package com.interviewcoach.interview.application.service;

import com.interviewcoach.interview.application.dto.request.StartInterviewRequest;
import com.interviewcoach.interview.application.dto.request.SubmitAnswerRequest;
import com.interviewcoach.interview.application.dto.response.InterviewListResponse;
import com.interviewcoach.interview.application.dto.response.InterviewSessionResponse;
import com.interviewcoach.interview.application.dto.response.QnaResponse;
import com.interviewcoach.interview.domain.entity.InterviewQna;
import com.interviewcoach.interview.domain.entity.InterviewSession;
import com.interviewcoach.interview.domain.repository.InterviewQnaRepository;
import com.interviewcoach.interview.domain.repository.InterviewSessionRepository;
import com.interviewcoach.interview.exception.InterviewAlreadyCompletedException;
import com.interviewcoach.interview.exception.InterviewNotFoundException;
import com.interviewcoach.interview.exception.QnaNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewService 단위 테스트")
class InterviewServiceTest {

    @Mock
    private InterviewSessionRepository sessionRepository;

    @Mock
    private InterviewQnaRepository qnaRepository;

    @InjectMocks
    private InterviewService interviewService;

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 100L;
    private static final Long JD_ID = 50L;

    @Nested
    @DisplayName("startInterview 메서드")
    class StartInterviewTest {

        @Test
        @DisplayName("면접 세션 시작 성공 - 질문 없이")
        void startInterview_WithoutQuestions_Success() throws Exception {
            // given
            StartInterviewRequest request = createStartRequest(JD_ID, "technical", null);
            InterviewSession savedSession = createSession(SESSION_ID, USER_ID, JD_ID, "technical", "in_progress");

            given(sessionRepository.save(any(InterviewSession.class))).willReturn(savedSession);

            // when
            InterviewSessionResponse response = interviewService.startInterview(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(SESSION_ID);
            assertThat(response.getJdId()).isEqualTo(JD_ID);
            assertThat(response.getInterviewType()).isEqualTo("technical");
            assertThat(response.getStatus()).isEqualTo("in_progress");
            verify(sessionRepository, times(1)).save(any(InterviewSession.class));
        }

        @Test
        @DisplayName("면접 세션 시작 성공 - 질문과 함께")
        void startInterview_WithQuestions_Success() throws Exception {
            // given
            List<StartInterviewRequest.QuestionInput> questions = List.of(
                    createQuestionInput("technical", "Java의 GC에 대해 설명해주세요."),
                    createQuestionInput("behavioral", "팀 갈등을 해결한 경험이 있나요?")
            );
            StartInterviewRequest request = createStartRequest(JD_ID, "mixed", questions);

            InterviewSession savedSession = createSession(SESSION_ID, USER_ID, JD_ID, "mixed", "in_progress");

            given(sessionRepository.save(any(InterviewSession.class))).willReturn(savedSession);

            // when
            InterviewSessionResponse response = interviewService.startInterview(USER_ID, request);

            // then
            assertThat(response).isNotNull();
            verify(sessionRepository, times(2)).save(any(InterviewSession.class)); // 초기 저장 + 질문 추가 후 저장
        }
    }

    @Nested
    @DisplayName("getInterviews 메서드")
    class GetInterviewsTest {

        @Test
        @DisplayName("사용자의 면접 목록 조회 성공")
        void getInterviews_Success() throws Exception {
            // given
            List<InterviewSession> sessions = List.of(
                    createSessionWithQnas(1L, USER_ID, JD_ID, "technical", "completed", 5),
                    createSessionWithQnas(2L, USER_ID, JD_ID, "behavioral", "in_progress", 3)
            );

            given(sessionRepository.findByUserIdWithQnaOrderByStartedAtDesc(USER_ID)).willReturn(sessions);

            // when
            InterviewListResponse response = interviewService.getInterviews(USER_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalCount()).isEqualTo(2);
            assertThat(response.getInterviews()).hasSize(2);
        }

        @Test
        @DisplayName("면접 이력이 없는 경우 빈 목록 반환")
        void getInterviews_Empty() {
            // given
            given(sessionRepository.findByUserIdWithQnaOrderByStartedAtDesc(USER_ID)).willReturn(List.of());

            // when
            InterviewListResponse response = interviewService.getInterviews(USER_ID);

            // then
            assertThat(response.getTotalCount()).isEqualTo(0);
            assertThat(response.getInterviews()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getInterview 메서드")
    class GetInterviewTest {

        @Test
        @DisplayName("면접 세션 단건 조회 성공")
        void getInterview_Success() throws Exception {
            // given
            InterviewSession session = createSessionWithQnas(SESSION_ID, USER_ID, JD_ID, "technical", "in_progress", 3);
            given(sessionRepository.findByIdWithQna(SESSION_ID)).willReturn(Optional.of(session));

            // when
            InterviewSessionResponse response = interviewService.getInterview(SESSION_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("존재하지 않는 세션 조회 시 예외 발생")
        void getInterview_NotFound() {
            // given
            given(sessionRepository.findByIdWithQna(SESSION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> interviewService.getInterview(SESSION_ID))
                    .isInstanceOf(InterviewNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("submitAnswer 메서드")
    class SubmitAnswerTest {

        @Test
        @DisplayName("답변 제출 성공")
        void submitAnswer_Success() throws Exception {
            // given
            SubmitAnswerRequest request = createSubmitRequest(1, "GC는 가비지 컬렉션으로...");

            InterviewSession session = createSession(SESSION_ID, USER_ID, JD_ID, "technical", "in_progress");
            InterviewQna qna = createQna(1L, session, 1, "technical", "Java의 GC에 대해 설명해주세요.");

            given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
            given(qnaRepository.findBySessionIdAndQuestionOrder(SESSION_ID, 1)).willReturn(Optional.of(qna));

            // when
            QnaResponse response = interviewService.submitAnswer(SESSION_ID, request);

            // then
            assertThat(response).isNotNull();
            assertThat(qna.getAnswerText()).isEqualTo("GC는 가비지 컬렉션으로...");
            // 피드백은 feedback-service에서 SSE로 전송 후 별도 API(updateFeedback)로 저장됨
            assertThat(qna.getAnsweredAt()).isNotNull();
        }

        @Test
        @DisplayName("완료된 면접에 답변 제출 시 예외 발생")
        void submitAnswer_AlreadyCompleted() throws Exception {
            // given
            SubmitAnswerRequest request = createSubmitRequest(1, "답변 내용");

            InterviewSession session = createSession(SESSION_ID, USER_ID, JD_ID, "technical", "completed");

            given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

            // when & then
            assertThatThrownBy(() -> interviewService.submitAnswer(SESSION_ID, request))
                    .isInstanceOf(InterviewAlreadyCompletedException.class);
        }

        @Test
        @DisplayName("존재하지 않는 질문에 답변 제출 시 예외 발생")
        void submitAnswer_QnaNotFound() throws Exception {
            // given
            SubmitAnswerRequest request = createSubmitRequest(99, "답변 내용");

            InterviewSession session = createSession(SESSION_ID, USER_ID, JD_ID, "technical", "in_progress");

            given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
            given(qnaRepository.findBySessionIdAndQuestionOrder(SESSION_ID, 99)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> interviewService.submitAnswer(SESSION_ID, request))
                    .isInstanceOf(QnaNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("completeInterview 메서드")
    class CompleteInterviewTest {

        @Test
        @DisplayName("면접 완료 성공")
        void completeInterview_Success() throws Exception {
            // given
            InterviewSession session = createSessionWithQnasAndFeedback(SESSION_ID, USER_ID, JD_ID, "technical", "in_progress");
            given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

            // when
            InterviewSessionResponse response = interviewService.completeInterview(SESSION_ID);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo("completed");
            assertThat(response.getAvgScore()).isNotNull();
        }

        @Test
        @DisplayName("이미 완료된 면접 완료 시도 시 예외 발생")
        void completeInterview_AlreadyCompleted() throws Exception {
            // given
            InterviewSession session = createSession(SESSION_ID, USER_ID, JD_ID, "technical", "completed");
            given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

            // when & then
            assertThatThrownBy(() -> interviewService.completeInterview(SESSION_ID))
                    .isInstanceOf(InterviewAlreadyCompletedException.class);
        }

        @Test
        @DisplayName("질문이 없는 면접 완료 시 평균 점수 0")
        void completeInterview_NoQuestions() throws Exception {
            // given
            InterviewSession session = createSession(SESSION_ID, USER_ID, JD_ID, "technical", "in_progress");
            given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

            // when
            InterviewSessionResponse response = interviewService.completeInterview(SESSION_ID);

            // then
            assertThat(response.getAvgScore()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("면접 기록 (History) 테스트")
    class InterviewHistoryTest {

        @Test
        @DisplayName("완료된 면접만 필터링하여 조회")
        void getInterviews_FilterCompleted() throws Exception {
            // given
            List<InterviewSession> sessions = List.of(
                    createSessionWithStatus(1L, USER_ID, "completed", 85),
                    createSessionWithStatus(2L, USER_ID, "in_progress", null),
                    createSessionWithStatus(3L, USER_ID, "completed", 72)
            );
            given(sessionRepository.findByUserIdWithQnaOrderByStartedAtDesc(USER_ID)).willReturn(sessions);

            // when
            InterviewListResponse response = interviewService.getInterviews(USER_ID);

            // then
            assertThat(response.getTotalCount()).isEqualTo(3);

            // 완료된 면접 개수 확인
            long completedCount = response.getInterviews().stream()
                    .filter(i -> "completed".equals(i.getStatus()))
                    .count();
            assertThat(completedCount).isEqualTo(2);
        }

        @Test
        @DisplayName("면접 기록이 최신순으로 정렬됨")
        void getInterviews_OrderedByStartedAtDesc() throws Exception {
            // given
            InterviewSession session1 = createSessionWithStartedAt(1L, USER_ID, LocalDateTime.now().minusDays(2));
            InterviewSession session2 = createSessionWithStartedAt(2L, USER_ID, LocalDateTime.now().minusDays(1));
            InterviewSession session3 = createSessionWithStartedAt(3L, USER_ID, LocalDateTime.now());

            // Repository는 이미 정렬된 결과를 반환한다고 가정
            List<InterviewSession> sessions = List.of(session3, session2, session1);
            given(sessionRepository.findByUserIdWithQnaOrderByStartedAtDesc(USER_ID)).willReturn(sessions);

            // when
            InterviewListResponse response = interviewService.getInterviews(USER_ID);

            // then
            assertThat(response.getInterviews()).hasSize(3);
            assertThat(response.getInterviews().get(0).getId()).isEqualTo(3L);
            assertThat(response.getInterviews().get(1).getId()).isEqualTo(2L);
            assertThat(response.getInterviews().get(2).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("면접 상세 조회 시 QnA 목록 포함")
        void getInterview_IncludesQnaList() throws Exception {
            // given
            InterviewSession session = createSessionWithQnasAndFeedback(SESSION_ID, USER_ID, JD_ID, "technical", "completed");
            given(sessionRepository.findByIdWithQna(SESSION_ID)).willReturn(Optional.of(session));

            // when
            InterviewSessionResponse response = interviewService.getInterview(SESSION_ID);

            // then
            assertThat(response.getQnaList()).isNotNull();
            assertThat(response.getQnaList()).hasSize(3);

            // 각 QnA에 피드백이 있는지 확인
            for (QnaResponse qna : response.getQnaList()) {
                assertThat(qna.getFeedback()).isNotNull();
                assertThat(qna.getFeedback().get("score")).isNotNull();
                assertThat(qna.getAnswerText()).isNotNull();
            }
        }

        @Test
        @DisplayName("완료된 면접의 평균 점수가 올바르게 계산됨")
        void completeInterview_AvgScoreCalculatedCorrectly() throws Exception {
            // given
            InterviewSession session = createSessionWithVariousScores(SESSION_ID, USER_ID, JD_ID);
            given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

            // when
            InterviewSessionResponse response = interviewService.completeInterview(SESSION_ID);

            // then
            // 점수: 70, 80, 90 -> 평균 80
            assertThat(response.getAvgScore()).isEqualByComparingTo(new BigDecimal("80"));
        }

        @Test
        @DisplayName("피드백이 없는 QnA는 평균 점수 계산에서 제외")
        void completeInterview_ExcludesQnaWithoutFeedback() throws Exception {
            // given
            InterviewSession session = createSessionWithPartialFeedback(SESSION_ID, USER_ID, JD_ID);
            given(sessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

            // when
            InterviewSessionResponse response = interviewService.completeInterview(SESSION_ID);

            // then
            // 피드백 있는 QnA만 평균 계산: 80, 90 -> 평균 85
            assertThat(response.getAvgScore()).isEqualByComparingTo(new BigDecimal("85"));
        }
    }

    // Helper methods
    private StartInterviewRequest createStartRequest(Long jdId, String interviewType,
                                                      List<StartInterviewRequest.QuestionInput> questions) throws Exception {
        StartInterviewRequest request = new StartInterviewRequest();
        setField(request, "jdId", jdId);
        setField(request, "interviewType", interviewType);
        setField(request, "questions", questions);
        return request;
    }

    private StartInterviewRequest.QuestionInput createQuestionInput(String questionType, String questionText) throws Exception {
        StartInterviewRequest.QuestionInput input = new StartInterviewRequest.QuestionInput();
        setField(input, "questionType", questionType);
        setField(input, "questionText", questionText);
        return input;
    }

    private SubmitAnswerRequest createSubmitRequest(int questionOrder, String answerText) throws Exception {
        SubmitAnswerRequest request = new SubmitAnswerRequest();
        setField(request, "questionOrder", questionOrder);
        setField(request, "answerText", answerText);
        return request;
    }

    private InterviewSession createSession(Long id, Long userId, Long jdId, String interviewType, String status) throws Exception {
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .jdId(jdId)
                .interviewType(interviewType)
                .status(status)
                .build();
        setField(session, "id", id);
        setField(session, "startedAt", LocalDateTime.now());
        return session;
    }

    private InterviewSession createSessionWithQnas(Long id, Long userId, Long jdId, String interviewType,
                                                    String status, int qnaCount) throws Exception {
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .jdId(jdId)
                .interviewType(interviewType)
                .status(status)
                .qnaList(new ArrayList<>())
                .build();
        setField(session, "id", id);
        setField(session, "startedAt", LocalDateTime.now());

        for (int i = 1; i <= qnaCount; i++) {
            InterviewQna qna = createQna((long) i, session, i, "technical", "질문 " + i);
            session.getQnaList().add(qna);
        }

        return session;
    }

    private InterviewSession createSessionWithQnasAndFeedback(Long id, Long userId, Long jdId,
                                                               String interviewType, String status) throws Exception {
        InterviewSession session = createSessionWithQnas(id, userId, jdId, interviewType, status, 3);

        for (InterviewQna qna : session.getQnaList()) {
            qna.submitAnswer("테스트 답변");
            qna.setFeedback(Map.of("score", 80, "strengths", List.of("좋음"), "improvements", List.of("개선점")));
        }

        return session;
    }

    private InterviewQna createQna(Long id, InterviewSession session, int questionOrder,
                                    String questionType, String questionText) throws Exception {
        InterviewQna qna = InterviewQna.builder()
                .session(session)
                .questionOrder(questionOrder)
                .questionType(questionType)
                .questionText(questionText)
                .build();
        setField(qna, "id", id);
        return qna;
    }

    private InterviewSession createSessionWithStatus(Long id, Long userId, String status, Integer avgScore) throws Exception {
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .jdId(JD_ID)
                .interviewType("technical")
                .status(status)
                .qnaList(new ArrayList<>())
                .build();
        setField(session, "id", id);
        setField(session, "startedAt", LocalDateTime.now());
        if (avgScore != null) {
            setField(session, "avgScore", BigDecimal.valueOf(avgScore));
        }
        if ("completed".equals(status)) {
            setField(session, "completedAt", LocalDateTime.now());
        }
        return session;
    }

    private InterviewSession createSessionWithStartedAt(Long id, Long userId, LocalDateTime startedAt) throws Exception {
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .jdId(JD_ID)
                .interviewType("technical")
                .status("completed")
                .qnaList(new ArrayList<>())
                .build();
        setField(session, "id", id);
        setField(session, "startedAt", startedAt);
        setField(session, "completedAt", startedAt.plusMinutes(30));
        return session;
    }

    private InterviewSession createSessionWithVariousScores(Long id, Long userId, Long jdId) throws Exception {
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .jdId(jdId)
                .interviewType("technical")
                .status("in_progress")
                .qnaList(new ArrayList<>())
                .build();
        setField(session, "id", id);
        setField(session, "startedAt", LocalDateTime.now());

        int[] scores = {70, 80, 90};
        for (int i = 0; i < scores.length; i++) {
            InterviewQna qna = createQna((long) (i + 1), session, i + 1, "technical", "질문 " + (i + 1));
            qna.submitAnswer("테스트 답변 " + (i + 1));
            qna.setFeedback(Map.of("score", scores[i], "strengths", List.of("좋음"), "improvements", List.of("개선점")));
            session.getQnaList().add(qna);
        }

        return session;
    }

    private InterviewSession createSessionWithPartialFeedback(Long id, Long userId, Long jdId) throws Exception {
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .jdId(jdId)
                .interviewType("technical")
                .status("in_progress")
                .qnaList(new ArrayList<>())
                .build();
        setField(session, "id", id);
        setField(session, "startedAt", LocalDateTime.now());

        // 첫 번째 QnA: 피드백 없음 (답변만 제출)
        InterviewQna qna1 = createQna(1L, session, 1, "technical", "질문 1");
        qna1.submitAnswer("답변 1");
        session.getQnaList().add(qna1);

        // 두 번째 QnA: 점수 80
        InterviewQna qna2 = createQna(2L, session, 2, "technical", "질문 2");
        qna2.submitAnswer("답변 2");
        qna2.setFeedback(Map.of("score", 80, "strengths", List.of("좋음"), "improvements", List.of("개선점")));
        session.getQnaList().add(qna2);

        // 세 번째 QnA: 점수 90
        InterviewQna qna3 = createQna(3L, session, 3, "technical", "질문 3");
        qna3.submitAnswer("답변 3");
        qna3.setFeedback(Map.of("score", 90, "strengths", List.of("좋음"), "improvements", List.of("개선점")));
        session.getQnaList().add(qna3);

        return session;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
