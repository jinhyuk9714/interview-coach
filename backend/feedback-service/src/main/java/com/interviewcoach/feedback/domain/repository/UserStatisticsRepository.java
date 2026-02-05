package com.interviewcoach.feedback.domain.repository;

import com.interviewcoach.feedback.domain.entity.UserStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserStatisticsRepository extends JpaRepository<UserStatistics, Long> {

    // [동시성 이슈] 락 없이 조회 - 4주차에 아래 메서드로 교체 예정
    // @Lock(LockModeType.PESSIMISTIC_WRITE)
    // @Query("SELECT s FROM UserStatistics s WHERE s.userId = :userId AND s.skillCategory = :skillCategory")
    // Optional<UserStatistics> findByUserIdAndSkillCategoryWithLock(
    //     @Param("userId") Long userId, @Param("skillCategory") String skillCategory);

    Optional<UserStatistics> findByUserIdAndSkillCategory(Long userId, String skillCategory);

    List<UserStatistics> findByUserId(Long userId);

    List<UserStatistics> findByUserIdOrderByCorrectRateDesc(Long userId);
}
