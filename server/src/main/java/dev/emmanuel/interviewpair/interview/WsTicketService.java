package dev.emmanuel.interviewpair.interview;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

/**
 * Browsers cannot send an Authorization header when they open a WebSocket, and putting the login
 * token in the URL would leave it in server logs. Instead a logged-in user asks for a ticket over
 * the normal API, then opens the socket with the ticket. A ticket works once, for one session, and
 * expires after 30 seconds.
 */
@Service
public class WsTicketService {

    public static final Duration TTL = Duration.ofSeconds(30);

    public record Ticket(UUID userId, UUID sessionId) {}

    private record Entry(Ticket ticket, Instant expiresAt) {}

    private final Map<String, Entry> tickets = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public WsTicketService(Clock clock) {
        this.clock = clock;
    }

    public String issue(UUID userId, UUID sessionId) {
        purgeExpired();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(value, new Entry(new Ticket(userId, sessionId), clock.instant().plus(TTL)));
        return value;
    }

    /** Returns the ticket details and removes it, so a second attempt with the same value fails. */
    public Optional<Ticket> redeem(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Entry entry = tickets.remove(value);
        if (entry == null || !entry.expiresAt().isAfter(clock.instant())) {
            return Optional.empty();
        }
        return Optional.of(entry.ticket());
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        tickets.values().removeIf(entry -> !entry.expiresAt().isAfter(now));
    }
}
