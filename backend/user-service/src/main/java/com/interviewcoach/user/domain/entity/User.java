package com.interviewcoach.user.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String nickname;

    @Column(name = "target_position", length = 100)
    private String targetPosition;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String nickname, String targetPosition, Integer experienceYears) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (targetPosition != null) {
            this.targetPosition = targetPosition;
        }
        if (experienceYears != null) {
            this.experienceYears = experienceYears;
        }
    }
}
