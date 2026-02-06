package com.interviewcoach.interview.domain.repository;

import com.interviewcoach.interview.domain.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByUserId(Long userId);

    List<InterviewSession> findByUserIdAndStatus(Long userId, String status);

    List<InterviewSession> findByUserIdOrderByStartedAtDesc(Long userId);

    // [B-1] Fetch Join으로 N+1 문제 해결
    // Before: findByUserIdOrderByStartedAtDesc → 세션 N개마다 qnaList 추가 쿼리 (N+1)
    // After: 단일 쿼리로 세션 + QnA 한 번에 조회
    @Query("SELECT DISTINCT s FROM InterviewSession s LEFT JOIN FETCH s.qnaList WHERE s.userId = :userId ORDER BY s.startedAt DESC")
    List<InterviewSession> findByUserIdWithQnaOrderByStartedAtDesc(@Param("userId") Long userId);

    // [B-1] 단일 세션 조회도 Fetch Join 적용
    @Query("SELECT s FROM InterviewSession s LEFT JOIN FETCH s.qnaList WHERE s.id = :id")
    Optional<InterviewSession> findByIdWithQna(@Param("id") Long id);

    // [A-1] 면접 기록 검색 - 질문/답변에서 키워드 검색
    @Query("SELECT DISTINCT s FROM InterviewSession s LEFT JOIN FETCH s.qnaList q " +
           "WHERE s.userId = :userId AND " +
           "(LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(q.answerText) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY s.startedAt DESC")
    List<InterviewSession> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}
