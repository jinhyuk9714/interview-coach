package com.interviewcoach.interview.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InterviewListResponse {

    private int totalCount;
    private List<InterviewSessionResponse> interviews;
}
