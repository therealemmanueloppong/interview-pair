package dev.emmanuel.interviewpair.interview;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WsTicketServiceTest {

    /** A clock the test can move forward. */
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-02T12:00:00Z");

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private final WsTicketService tickets = new WsTicketService(clock);
    private final UUID user = UUID.randomUUID();
    private final UUID session = UUID.randomUUID();

    @Test
    void aTicketWorksOnceForTheRightPersonAndSession() {
        String ticket = tickets.issue(user, session);

        assertThat(tickets.redeem(ticket)).contains(new WsTicketService.Ticket(user, session));
        assertThat(tickets.redeem(ticket)).isEmpty();
    }

    @Test
    void anExpiredTicketIsRejected() {
        String ticket = tickets.issue(user, session);
        clock.now = clock.now.plus(WsTicketService.TTL).plusSeconds(1);

        assertThat(tickets.redeem(ticket)).isEmpty();
    }

    @Test
    void aTicketJustBeforeExpiryStillWorks() {
        String ticket = tickets.issue(user, session);
        clock.now = clock.now.plus(WsTicketService.TTL).minus(Duration.ofSeconds(1));

        assertThat(tickets.redeem(ticket)).isPresent();
    }

    @Test
    void unknownOrMissingTicketsAreRejected() {
        assertThat(tickets.redeem("not-a-ticket")).isEmpty();
        assertThat(tickets.redeem(null)).isEmpty();
    }

    @Test
    void ticketsAreLongRandomAndDifferent() {
        String a = tickets.issue(user, session);
        String b = tickets.issue(user, session);

        assertThat(a).isNotEqualTo(b).hasSizeGreaterThanOrEqualTo(40);
    }
}
