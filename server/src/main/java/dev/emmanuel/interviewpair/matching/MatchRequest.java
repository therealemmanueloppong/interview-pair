package dev.emmanuel.interviewpair.matching;

import java.time.LocalDateTime;
import java.util.UUID;

public record MatchRequest(
        UUID userId, String topic, String level, LocalDateTime startsAt, LocalDateTime endsAt) {

    public MatchRequest {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
    }
}
