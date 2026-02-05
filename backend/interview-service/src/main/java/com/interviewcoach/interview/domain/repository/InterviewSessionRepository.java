package com.interviewcoach.interview.domain.repository;

import com.interviewcoach.interview.domain.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    // [N+1 발생 포인트] 세션 목록만 조회하고 qnaList는 LAZY 로딩
    // 5주차에 아래 메서드로 교체 예정:
    // @Query("SELECT s FROM InterviewSession s LEFT JOIN FETCH s.qnaList WHERE s.userId = :userId")
    // List<InterviewSession> findByUserIdWithQna(@Param("userId") Long userId);
    List<InterviewSession> findByUserId(Long userId);

    List<InterviewSession> findByUserIdAndStatus(Long userId, String status);

    List<InterviewSession> findByUserIdOrderByStartedAtDesc(Long userId);
}
