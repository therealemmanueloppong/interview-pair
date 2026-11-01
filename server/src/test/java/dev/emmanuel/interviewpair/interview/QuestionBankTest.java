package dev.emmanuel.interviewpair.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestionBankTest {

    private final QuestionBank bank = new QuestionBank();

    @Test
    void everyTopicAndLevelHasAtLeastTwoQuestions() {
        for (Topic topic : Topic.values()) {
            for (Level level : Level.values()) {
                long count = bank.all().stream().filter(q -> q.topic() == topic && q.level() == level).count();
                assertThat(count).as("%s %s", topic, level).isGreaterThanOrEqualTo(2);
            }
        }
    }

    @Test
    void idsAreUniqueAndEveryQuestionHasGuidanceForTheInterviewer() {
        Set<String> ids = new HashSet<>();
        for (Question q : bank.all()) {
            assertThat(ids.add(q.id())).as("duplicate id %s", q.id()).isTrue();
            assertThat(q.title()).isNotBlank();
            assertThat(q.prompt()).isNotBlank();
            assertThat(q.hints()).as(q.id()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(q.solution()).as(q.id()).isNotBlank();
        }
    }

    @Test
    void codingQuestionsHaveStarterCodeAndTheOthersDoNot() {
        for (Question q : bank.all()) {
            if (q.topic() == Topic.ALGORITHMS) {
                assertThat(q.starterCode()).as(q.id()).contains("def ").isNotBlank();
            } else {
                assertThat(q.starterCode()).as(q.id()).isEmpty();
            }
        }
    }

    @Test
    void pickTwoGivesTwoDifferentQuestionsForTheRequestedTopicAndLevel() {
        for (int i = 0; i < 50; i++) {
            List<Question> picked = bank.pickTwo(Topic.ALGORITHMS, Level.INTERMEDIATE, new Random());
            assertThat(picked).hasSize(2);
            assertThat(picked.get(0).id()).isNotEqualTo(picked.get(1).id());
            assertThat(picked).allMatch(q -> q.topic() == Topic.ALGORITHMS && q.level() == Level.INTERMEDIATE);
        }
    }

    @Test
    void getFailsLoudlyForAnUnknownQuestion() {
        assertThatThrownBy(() -> bank.get("nope")).isInstanceOf(IllegalStateException.class);
    }
}
