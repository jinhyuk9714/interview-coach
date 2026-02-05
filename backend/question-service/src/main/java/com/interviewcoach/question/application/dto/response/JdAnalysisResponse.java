package com.interviewcoach.question.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class JdAnalysisResponse {

    private Long jdId;
    private List<String> skills;
    private List<String> requirements;
    private String summary;
}
