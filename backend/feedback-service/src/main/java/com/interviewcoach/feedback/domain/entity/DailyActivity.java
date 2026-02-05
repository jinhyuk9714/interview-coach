package com.interviewcoach.feedback.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_activity",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "activity_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "question_count")
    @Builder.Default
    private Integer questionCount = 0;

    @Column(name = "total_score")
    @Builder.Default
    private Integer totalScore = 0;

    @Column(name = "interview_count")
    @Builder.Default
    private Integer interviewCount = 0;

    public void recordActivity(int score) {
        this.questionCount = this.questionCount + 1;
        this.totalScore = this.totalScore + score;
    }

    public void incrementInterviewCount() {
        this.interviewCount = this.interviewCount + 1;
    }

    public int getAverageScore() {
        if (questionCount == 0) return 0;
        return totalScore / questionCount;
    }
}
