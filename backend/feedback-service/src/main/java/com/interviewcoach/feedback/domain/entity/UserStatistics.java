package com.interviewcoach.feedback.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_statistics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill_category"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "skill_category", nullable = false, length = 50)
    private String skillCategory;

    @Column(name = "total_questions")
    @Builder.Default
    private Integer totalQuestions = 0;

    @Column(name = "correct_count")
    @Builder.Default
    private Integer correctCount = 0;

    @Column(name = "total_score")
    @Builder.Default
    private Integer totalScore = 0;

    @Column(name = "correct_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal correctRate = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weak_points", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> weakPoints = new ArrayList<>();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * [동시성 이슈 포인트] - 락 없이 비원자적 읽기-수정-쓰기 연산
     * 4주차에 비관적 락(@Lock(LockModeType.PESSIMISTIC_WRITE))으로 최적화 예정
     *
     * 문제: 동시에 여러 요청이 들어오면 일부 업데이트가 손실될 수 있음
     * 예: Thread1 읽기(10) -> Thread2 읽기(10) -> Thread1 쓰기(11) -> Thread2 쓰기(11)
     *     결과: 10 -> 11 (예상: 12)
     */
    public void recordAnswer(boolean isCorrect) {
        recordAnswer(isCorrect, isCorrect ? 100 : 0);
    }

    /**
     * 점수 기반 답변 기록
     */
    public void recordAnswer(boolean isCorrect, int score) {
        // Race condition 발생 가능 지점!
        this.totalQuestions = this.totalQuestions + 1;
        this.totalScore = this.totalScore + score;

        if (isCorrect) {
            this.correctCount = this.correctCount + 1;
        }

        // 평균 점수 재계산 (correctRate를 평균 점수로 사용)
        if (this.totalQuestions > 0) {
            this.correctRate = BigDecimal.valueOf(this.totalScore)
                    .divide(BigDecimal.valueOf(this.totalQuestions), 2, RoundingMode.HALF_UP);
        }
    }

    public void addWeakPoint(String weakPoint) {
        if (this.weakPoints == null) {
            this.weakPoints = new ArrayList<>();
        }
        if (!this.weakPoints.contains(weakPoint)) {
            this.weakPoints.add(weakPoint);
        }
    }
}
