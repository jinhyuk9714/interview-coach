package com.interviewcoach.interview.exception;

public class InterviewNotFoundException extends RuntimeException {

    public InterviewNotFoundException(Long sessionId) {
        super("면접 세션을 찾을 수 없습니다: " + sessionId);
    }
}
