package com.interviewcoach.interview.domain.repository;

import com.interviewcoach.interview.domain.entity.InterviewQna;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InterviewQnaRepository extends JpaRepository<InterviewQna, Long> {

    List<InterviewQna> findBySessionIdOrderByQuestionOrderAsc(Long sessionId);

    Optional<InterviewQna> findBySessionIdAndQuestionOrder(Long sessionId, Integer questionOrder);

    long countBySessionId(Long sessionId);
}
