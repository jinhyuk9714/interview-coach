package com.interviewcoach.question.domain.repository;

import com.interviewcoach.question.domain.entity.GeneratedQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeneratedQuestionRepository extends JpaRepository<GeneratedQuestion, Long> {

    List<GeneratedQuestion> findByJdId(Long jdId);

    List<GeneratedQuestion> findByJdIdAndQuestionType(Long jdId, String questionType);

    void deleteByJdId(Long jdId);
}
