package com.interviewcoach.question.domain.repository;

import com.interviewcoach.question.domain.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {

    // 인덱스 없이 조회 - 의도적으로 인덱스 미활용 (3주차 최적화 대상)
    List<JobDescription> findByUserId(Long userId);

    List<JobDescription> findByUserIdOrderByCreatedAtDesc(Long userId);
}
