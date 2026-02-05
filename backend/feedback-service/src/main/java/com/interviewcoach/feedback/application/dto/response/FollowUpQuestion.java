package com.interviewcoach.feedback.application.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FollowUpQuestion {
    private String questionText;
    private String focusArea;
    private String rationale;
    private Boolean shouldAsk;
}
