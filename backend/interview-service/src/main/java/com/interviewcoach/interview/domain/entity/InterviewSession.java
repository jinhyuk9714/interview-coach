package com.interviewcoach.interview.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "jd_id")
    private Long jdId;

    @Column(name = "interview_type", nullable = false, length = 50)
    private String interviewType; // technical, behavioral, mixed

    @Column(length = 20)
    @Builder.Default
    private String status = "in_progress"; // in_progress, paused, completed, cancelled

    @Column(name = "total_questions")
    @Builder.Default
    private Integer totalQuestions = 0;

    @Column(name = "avg_score", precision = 3, scale = 1)
    private BigDecimal avgScore;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // [N+1 포인트] LAZY 로딩으로 세션 목록 조회 시 N+1 발생
    // 5주차에 Fetch Join / @EntityGraph로 최적화 예정
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InterviewQna> qnaList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.startedAt = LocalDateTime.now();
    }

    public void addQna(InterviewQna qna) {
        this.qnaList.add(qna);
        qna.setSession(this);
        this.totalQuestions = this.qnaList.size();
    }

    public void complete(BigDecimal avgScore) {
        this.status = "completed";
        this.avgScore = avgScore;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = "cancelled";
        this.completedAt = LocalDateTime.now();
    }

    public boolean isInProgress() {
        return "in_progress".equals(this.status);
    }

    public void pause() {
        this.status = "paused";
    }

    public void resume() {
        this.status = "in_progress";
    }

    public boolean isPaused() {
        return "paused".equals(this.status);
    }

    public boolean canAnswer() {
        return "in_progress".equals(this.status);
    }
}
