package com.interviewcoach.interview.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "interview_qna")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InterviewQna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @Setter
    private InterviewSession session;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "question_type", length = 50)
    private String questionType; // technical, behavioral, follow_up

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "answer_text", columnDefinition = "TEXT")
    private String answerText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> feedback; // {score, strengths, improvements, tips}

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Column(name = "parent_qna_id")
    private Long parentQnaId;

    @Column(name = "follow_up_depth")
    @Builder.Default
    private Integer followUpDepth = 0;

    @Column(name = "is_follow_up")
    @Builder.Default
    private Boolean isFollowUp = false;

    public void submitAnswer(String answerText) {
        this.answerText = answerText;
        this.answeredAt = LocalDateTime.now();
    }

    public void setFeedback(Map<String, Object> feedback) {
        this.feedback = feedback;
    }

    public boolean isAnswered() {
        return this.answerText != null && !this.answerText.isBlank();
    }
}
