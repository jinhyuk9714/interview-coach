package com.interviewcoach.interview.exception;

public class InterviewAlreadyCompletedException extends RuntimeException {

    public InterviewAlreadyCompletedException(Long sessionId) {
        super("이미 완료된 면접 세션입니다: " + sessionId);
    }
}
