package com.interviewcoach.feedback.domain.repository;

import com.interviewcoach.feedback.domain.entity.DailyActivity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyActivityRepository extends JpaRepository<DailyActivity, Long> {

    Optional<DailyActivity> findByUserIdAndActivityDate(Long userId, LocalDate activityDate);

    // [B-3] 비관적 락 적용 - 동시 활동 기록 시 Lost Update 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DailyActivity d WHERE d.userId = :userId AND d.activityDate = :activityDate")
    Optional<DailyActivity> findByUserIdAndActivityDateWithLock(
            @Param("userId") Long userId, @Param("activityDate") LocalDate activityDate);

    @Query("SELECT d FROM DailyActivity d WHERE d.userId = :userId AND d.activityDate >= :startDate ORDER BY d.activityDate ASC")
    List<DailyActivity> findByUserIdAndActivityDateAfter(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate);

    @Query("SELECT d FROM DailyActivity d WHERE d.userId = :userId AND d.activityDate BETWEEN :startDate AND :endDate ORDER BY d.activityDate ASC")
    List<DailyActivity> findByUserIdAndActivityDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
