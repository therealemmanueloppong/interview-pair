package dev.emmanuel.interviewpair.interview;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Every test class shares one in-memory database, so each test takes its own slot. Two tests then
 * never see each other's waiting requests.
 */
final class TestSlots {

    private static final AtomicInteger NEXT = new AtomicInteger();

    private TestSlots() {}

    /** A valid two-hour slot between 2 and 6 days from now that nobody else in the test run has used. */
    static Instant unique() {
        Instant start = Instant.now().plus(Duration.ofDays(2)).truncatedTo(ChronoUnit.HOURS);
        if (start.atZone(ZoneOffset.UTC).getHour() % 2 != 0) {
            start = start.plus(1, ChronoUnit.HOURS);
        }
        return start.plus(Duration.ofHours(2L * (NEXT.getAndIncrement() % 70)));
    }
}
