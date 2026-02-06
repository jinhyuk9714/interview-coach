package com.interviewcoach.feedback.domain.repository;

import com.interviewcoach.feedback.domain.entity.UserStatistics;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

    Optional<UserStatistics> findByUserIdAndSkillCategory(Long userId, String skillCategory);

    // [B-3] 비관적 락 적용 - Lost Update 방지
    // Before: 50 동시 요청 시 ~30% 통계 불일치 (Lost Update)
    // After: PESSIMISTIC_WRITE로 SELECT ... FOR UPDATE 실행 → 100% 정합성
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserStatistics s WHERE s.userId = :userId AND s.skillCategory = :skillCategory")
    Optional<UserStatistics> findByUserIdAndSkillCategoryWithLock(
            @Param("userId") Long userId, @Param("skillCategory") String skillCategory);

    List<UserStatistics> findByUserId(Long userId);

    List<UserStatistics> findByUserIdOrderByCorrectRateDesc(Long userId);
}
