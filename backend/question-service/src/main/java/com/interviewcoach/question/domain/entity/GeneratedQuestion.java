package com.interviewcoach.question.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "generated_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GeneratedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jd_id")
    private Long jdId;

    @Column(name = "question_type", length = 50)
    private String questionType; // technical, behavioral, situational

    @Column(name = "skill_category", length = 50)
    private String skillCategory;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String hint;

    @Column(name = "ideal_answer", columnDefinition = "TEXT")
    private String idealAnswer;

    @Column
    private Integer difficulty; // 1-5

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
