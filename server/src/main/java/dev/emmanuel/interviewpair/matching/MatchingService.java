package dev.emmanuel.interviewpair.matching;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Pairs two students who want the same topic and level and whose time windows overlap
 * for at least {@link #MIN_OVERLAP}. Among valid partners the longest overlap wins.
 */
@Service
public class MatchingService {

    public static final Duration MIN_OVERLAP = Duration.ofMinutes(45);

    public Optional<MatchRequest> findPartner(MatchRequest request, List<MatchRequest> pool) {
        return pool.stream()
                .filter(other -> !other.userId().equals(request.userId()))
                .filter(other -> other.topic().equalsIgnoreCase(request.topic()))
                .filter(other -> other.level().equalsIgnoreCase(request.level()))
                .filter(other -> overlap(request, other).compareTo(MIN_OVERLAP) >= 0)
                .max(Comparator.comparing((MatchRequest other) -> overlap(request, other))
                        .thenComparing(MatchRequest::startsAt, Comparator.reverseOrder()));
    }

    static Duration overlap(MatchRequest a, MatchRequest b) {
        LocalDateTime start = a.startsAt().isAfter(b.startsAt()) ? a.startsAt() : b.startsAt();
        LocalDateTime end = a.endsAt().isBefore(b.endsAt()) ? a.endsAt() : b.endsAt();
        return end.isAfter(start) ? Duration.between(start, end) : Duration.ZERO;
    }
}
