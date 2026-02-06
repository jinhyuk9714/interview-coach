package com.interviewcoach.question.application.dto.response;

import com.interviewcoach.question.domain.entity.JobDescription;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class JdResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String companyName;
    private String position;
    private String originalText;
    private String originalUrl;
    private List<String> parsedSkills;
    private List<String> parsedRequirements;
    private LocalDateTime createdAt;

    public static JdResponse from(JobDescription jd) {
        return JdResponse.builder()
                .id(jd.getId())
                .userId(jd.getUserId())
                .companyName(jd.getCompanyName())
                .position(jd.getPosition())
                .originalText(jd.getOriginalText())
                .originalUrl(jd.getOriginalUrl())
                .parsedSkills(jd.getParsedSkills())
                .parsedRequirements(jd.getParsedRequirements())
                .createdAt(jd.getCreatedAt())
                .build();
    }
}
