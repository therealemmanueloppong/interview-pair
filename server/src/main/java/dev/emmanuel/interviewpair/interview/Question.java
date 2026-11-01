package dev.emmanuel.interviewpair.interview;

import java.util.List;

/**
 * One interview question. The hints and the solution are written for the interviewer, so the server
 * only sends them to the person who is not answering the question.
 */
public record Question(
        String id,
        Topic topic,
        Level level,
        String title,
        String prompt,
        String starterCode,
        List<String> hints,
        String solution) {}
