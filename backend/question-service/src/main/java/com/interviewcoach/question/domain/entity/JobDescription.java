package com.interviewcoach.question.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_descriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_name", length = 100)
    private String companyName;

    @Column(length = 100)
    private String position;

    @Column(name = "original_text", columnDefinition = "TEXT", nullable = false)
    private String originalText;

    @Column(name = "original_url", length = 500)
    private String originalUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_skills", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> parsedSkills = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_requirements", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> parsedRequirements = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updateParsedData(List<String> skills, List<String> requirements) {
        this.parsedSkills = skills != null ? skills : new ArrayList<>();
        this.parsedRequirements = requirements != null ? requirements : new ArrayList<>();
    }
}
