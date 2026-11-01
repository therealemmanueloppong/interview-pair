package dev.emmanuel.interviewpair.interview;

import java.util.UUID;

/** Published after a session's roles or status change, so connected people get the new state. */
public record SessionChanged(UUID sessionId) {}
