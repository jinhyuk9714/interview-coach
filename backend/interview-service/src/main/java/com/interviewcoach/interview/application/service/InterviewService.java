package com.interviewcoach.interview.application.service;

import com.interviewcoach.interview.application.dto.request.AddFollowUpRequest;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewSessionRepository sessionRepository;
    private final InterviewQnaRepository qnaRepository;

    @Transactional
    public InterviewSessionResponse startInterview(Long userId, StartInterviewRequest request) {
        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .jdId(request.getJdId())
                .interviewType(request.getInterviewType())
                .build();

        InterviewSession savedSession = sessionRepository.save(session);

        // 질문이 제공된 경우 추가
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            int order = 1;
            for (StartInterviewRequest.QuestionInput q : request.getQuestions()) {
                InterviewQna qna = InterviewQna.builder()
                        .questionOrder(order++)
                        .questionType(q.getQuestionType())
                        .questionText(q.getQuestionText())
                        .build();
                savedSession.addQna(qna);
            }
            sessionRepository.save(savedSession);
        }

        log.info("Started interview session: id={}, userId={}, jdId={}, type={}",
                savedSession.getId(), userId, request.getJdId(), request.getInterviewType());

        return InterviewSessionResponse.fromWithQna(savedSession);
    }

    @Transactional(readOnly = true)
    public InterviewListResponse getInterviews(Long userId) {
        // [B-1] Fetch Join으로 N+1 문제 해결
        // Before: findByUserIdOrderByStartedAtDesc → 세션 N개마다 qnaList 추가 쿼리 (11 queries for 10 sessions)
        // After: findByUserIdWithQnaOrderByStartedAtDesc → 단일 쿼리 (1 query)
        List<InterviewSession> sessions = sessionRepository.findByUserIdWithQnaOrderByStartedAtDesc(userId);

        List<InterviewSessionResponse> responses = sessions.stream()
                .map(InterviewSessionResponse::fromWithQna)
                .toList();

        return InterviewListResponse.builder()
                .totalCount(responses.size())
                .interviews(responses)
                .build();
    }

    @Transactional(readOnly = true)
    public InterviewSessionResponse getInterview(Long sessionId) {
        // [B-1] 단일 세션 조회도 Fetch Join 적용
        InterviewSession session = sessionRepository.findByIdWithQna(sessionId)
                .orElseThrow(() -> new InterviewNotFoundException(sessionId));
        return InterviewSessionResponse.fromWithQna(session);
    }

    // [A-1] 면접 기록 검색
    @Transactional(readOnly = true)
    public InterviewListResponse searchInterviews(Long userId, String keyword) {
        List<InterviewSession> sessions = sessionRepository.searchByKeyword(userId, keyword);

        List<InterviewSessionResponse> responses = sessions.stream()
                .map(InterviewSessionResponse::fromWithQna)
                .toList();

        return InterviewListResponse.builder()
                .totalCount(responses.size())
                .interviews(responses)
                .build();
    }

    @Transactional
    public QnaResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new InterviewNotFoundException(sessionId));

        if (!session.canAnswer()) {
            throw new InterviewAlreadyCompletedException(sessionId);
        }

        InterviewQna qna = qnaRepository.findBySessionIdAndQuestionOrder(sessionId, request.getQuestionOrder())
                .orElseThrow(() -> new QnaNotFoundException(sessionId, request.getQuestionOrder()));

        qna.submitAnswer(request.getAnswerText());

        // 피드백은 feedback-service에서 SSE로 전송 후 별도 API로 저장됨
        log.info("Submitted answer: sessionId={}, questionOrder={}", sessionId, request.getQuestionOrder());

        return QnaResponse.from(qna);
    }

    @Transactional
    public QnaResponse updateFeedback(Long sessionId, Integer questionOrder, Map<String, Object> feedback) {
        InterviewQna qna = qnaRepository.findBySessionIdAndQuestionOrder(sessionId, questionOrder)
                .orElseThrow(() -> new QnaNotFoundException(sessionId, questionOrder));

        qna.setFeedback(feedback);
        log.info("Updated feedback: sessionId={}, questionOrder={}, score={}",
                sessionId, questionOrder, feedback.get("score"));

        return QnaResponse.from(qna);
    }

    @Transactional
    public InterviewSessionResponse completeInterview(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new InterviewNotFoundException(sessionId));

        if (!session.isInProgress() && !session.isPaused()) {
            throw new InterviewAlreadyCompletedException(sessionId);
        }

        // 평균 점수 계산
        List<InterviewQna> qnaList = session.getQnaList();
        BigDecimal avgScore = calculateAvgScore(qnaList);

        session.complete(avgScore);
        sessionRepository.save(session);

        log.info("Completed interview: sessionId={}, avgScore={}", sessionId, avgScore);

        return InterviewSessionResponse.fromWithQna(session);
    }

    // [A-2] 면접 일시정지
    @Transactional
    public InterviewSessionResponse pauseInterview(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new InterviewNotFoundException(sessionId));

        if (!session.isInProgress()) {
            throw new InterviewAlreadyCompletedException(sessionId);
        }

        session.pause();
        log.info("Paused interview: sessionId={}", sessionId);

        return InterviewSessionResponse.fromWithQna(session);
    }

    // [A-2] 면접 재개
    @Transactional
    public InterviewSessionResponse resumeInterview(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new InterviewNotFoundException(sessionId));

        if (!session.isPaused()) {
            throw new IllegalStateException("면접이 일시정지 상태가 아닙니다. sessionId=" + sessionId);
        }

        session.resume();
        log.info("Resumed interview: sessionId={}", sessionId);

        return InterviewSessionResponse.fromWithQna(session);
    }

    @Transactional
    public QnaResponse addFollowUpQuestion(Long sessionId, AddFollowUpRequest request) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new InterviewNotFoundException(sessionId));

        if (!session.canAnswer()) {
            throw new InterviewAlreadyCompletedException(sessionId);
        }

        InterviewQna parentQna = qnaRepository.findById(request.getParentQnaId())
                .orElseThrow(() -> new QnaNotFoundException(sessionId, request.getParentQnaId().intValue()));

        int nextOrder = session.getQnaList().size() + 1;

        InterviewQna followUpQna = InterviewQna.builder()
                .questionOrder(nextOrder)
                .questionType("follow_up")
                .questionText(request.getQuestionText())
                .parentQnaId(request.getParentQnaId())
                .followUpDepth(request.getFollowUpDepth())
                .isFollowUp(true)
                .build();

        session.addQna(followUpQna);
        sessionRepository.save(session);

        log.info("Added follow-up question to session={}, parentQnaId={}, depth={}",
                sessionId, request.getParentQnaId(), request.getFollowUpDepth());

        return QnaResponse.from(followUpQna);
    }

    private BigDecimal calculateAvgScore(List<InterviewQna> qnaList) {
        if (qnaList.isEmpty()) {
            return BigDecimal.ZERO;
        }

        double totalScore = qnaList.stream()
                .filter(qna -> qna.getFeedback() != null)
                .mapToDouble(qna -> {
                    Object score = qna.getFeedback().get("score");
                    return score instanceof Number ? ((Number) score).doubleValue() : 0;
                })
                .sum();

        long count = qnaList.stream()
                .filter(qna -> qna.getFeedback() != null && qna.getFeedback().get("score") != null)
                .count();

        if (count == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(totalScore / count).setScale(1, RoundingMode.HALF_UP);
    }
}
