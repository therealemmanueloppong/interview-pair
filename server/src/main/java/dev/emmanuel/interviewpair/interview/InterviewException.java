package dev.emmanuel.interviewpair.interview;

import org.springframework.http.HttpStatus;

/** A problem the user can understand, such as a slot in the past or a session that is not theirs. */
public class InterviewException extends RuntimeException {

    private final HttpStatus status;

    public InterviewException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
