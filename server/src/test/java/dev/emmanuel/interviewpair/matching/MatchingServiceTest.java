package dev.emmanuel.interviewpair.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchingServiceTest {

    private final MatchingService service = new MatchingService();
    private static final LocalDateTime NINE = LocalDateTime.of(2026, 11, 2, 9, 0);

    private static MatchRequest req(String topic, String level, int startMin, int endMin) {
        return new MatchRequest(UUID.randomUUID(), topic, level, NINE.plusMinutes(startMin), NINE.plusMinutes(endMin));
    }

    @Test
    void matchesSameTopicAndLevelWithEnoughOverlap() {
        MatchRequest me = req("graphs", "medium", 0, 90);
        MatchRequest partner = req("graphs", "medium", 30, 120);
        assertThat(service.findPartner(me, List.of(partner))).contains(partner);
    }

    @Test
    void rejectsOverlapShorterThanMinimum() {
        MatchRequest me = req("graphs", "medium", 0, 60);
        MatchRequest tooShort = req("graphs", "medium", 30, 120);
        assertThat(service.findPartner(me, List.of(tooShort))).isEmpty();
    }

    @Test
    void rejectsDifferentTopicOrLevel() {
        MatchRequest me = req("graphs", "medium", 0, 90);
        assertThat(service.findPartner(me, List.of(req("dp", "medium", 0, 90)))).isEmpty();
        assertThat(service.findPartner(me, List.of(req("graphs", "hard", 0, 90)))).isEmpty();
    }

    @Test
    void neverMatchesAUserWithThemselves() {
        MatchRequest me = req("graphs", "medium", 0, 90);
        assertThat(service.findPartner(me, List.of(me))).isEmpty();
    }

    @Test
    void picksTheLongestOverlap() {
        MatchRequest me = req("graphs", "medium", 0, 120);
        MatchRequest short1 = req("graphs", "medium", 60, 120);
        MatchRequest longer = req("graphs", "medium", 0, 110);
        assertThat(service.findPartner(me, List.of(short1, longer))).contains(longer);
    }
}
