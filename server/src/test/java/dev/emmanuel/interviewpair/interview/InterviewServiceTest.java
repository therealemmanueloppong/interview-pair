package dev.emmanuel.interviewpair.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.emmanuel.interviewpair.interview.InterviewDtos.RequestView;
import dev.emmanuel.interviewpair.interview.InterviewDtos.SessionView;
import dev.emmanuel.interviewpair.user.User;
import dev.emmanuel.interviewpair.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

@SpringBootTest
class InterviewServiceTest {

    @Autowired InterviewService service;
    @Autowired UserRepository users;
    @Autowired QuestionBank bank;

    private UUID newUser(String name) {
        UUID id = UUID.randomUUID();
        users.saveAndFlush(new User(id, id + "@example.com", "hash", name, LocalDateTime.now()));
        return id;
    }

    /** Two people ask for the same thing, so the second request creates the session. */
    private UUID matchedSession(UUID first, UUID second, Instant slot) {
        service.requestInterview(first, Topic.ALGORITHMS, Level.BEGINNER, slot);
        RequestView view = service.requestInterview(second, Topic.ALGORITHMS, Level.BEGINNER, slot);
        assertThat(view.sessionId()).isNotNull();
        return view.sessionId();
    }

    private void assertFails(HttpStatus status, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(InterviewException.class, e -> assertThat(e.status()).isEqualTo(status));
    }

    // -------------------------------------------------------------- matching

    @Test
    void twoPeopleWithTheSameTopicLevelAndSlotAreMatched() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        Instant slot = TestSlots.unique();

        RequestView first = service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, slot);
        assertThat(first.status()).isEqualTo(RequestStatus.OPEN);
        assertThat(first.sessionId()).isNull();

        RequestView second = service.requestInterview(grace, Topic.ALGORITHMS, Level.BEGINNER, slot);
        assertThat(second.status()).isEqualTo(RequestStatus.MATCHED);

        SessionView forAda = service.getSession(ada, second.sessionId());
        SessionView forGrace = service.getSession(grace, second.sessionId());
        assertThat(forAda.partner().displayName()).isEqualTo("Grace");
        assertThat(forGrace.partner().displayName()).isEqualTo("Ada");
        // The person who asked first answers first.
        assertThat(forAda.youAreInterviewee()).isTrue();
        assertThat(forGrace.youAreInterviewee()).isFalse();
        // The first request is closed too, so it is no longer waiting.
        assertThat(service.openRequests(ada)).isEmpty();
    }

    @Test
    void differentLevelsOrDifferentSlotsDoNotMatch() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        Instant slot = TestSlots.unique();

        service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, slot);
        RequestView otherLevel = service.requestInterview(grace, Topic.ALGORITHMS, Level.ADVANCED, slot);
        // A different slot of its own, so this test cannot leave a waiting request in someone else's slot.
        RequestView otherSlot =
                service.requestInterview(grace, Topic.ALGORITHMS, Level.BEGINNER, TestSlots.unique());

        assertThat(otherLevel.sessionId()).isNull();
        assertThat(otherSlot.sessionId()).isNull();
        assertThat(service.openRequests(ada)).hasSize(1);
    }

    @Test
    void differentTopicsDoNotMatch() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        Instant slot = TestSlots.unique();

        service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, slot);
        RequestView other = service.requestInterview(grace, Topic.BEHAVIORAL, Level.BEGINNER, slot);

        assertThat(other.sessionId()).isNull();
    }

    @Test
    void youCannotBookTheSameSlotTwiceOrMatchYourself() {
        UUID ada = newUser("Ada");
        Instant slot = TestSlots.unique();

        service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, slot);

        assertFails(HttpStatus.CONFLICT,
                () -> service.requestInterview(ada, Topic.BEHAVIORAL, Level.BEGINNER, slot));
    }

    @Test
    void youCannotBookASlotThatAlreadyHasASession() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID linus = newUser("Linus");
        Instant slot = TestSlots.unique();
        matchedSession(ada, grace, slot);

        assertFails(HttpStatus.CONFLICT,
                () -> service.requestInterview(ada, Topic.SYSTEM_DESIGN, Level.BEGINNER, slot));
        // A third person can still wait for a new partner in that slot.
        assertThat(service.requestInterview(linus, Topic.ALGORITHMS, Level.BEGINNER, slot).sessionId()).isNull();
    }

    @Test
    void aCancelledRequestIsNotMatched() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        Instant slot = TestSlots.unique();

        RequestView waiting = service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, slot);
        service.cancelRequest(ada, waiting.id());
        RequestView later = service.requestInterview(grace, Topic.ALGORITHMS, Level.BEGINNER, slot);

        assertThat(later.sessionId()).isNull();
        assertThat(service.openRequests(ada)).isEmpty();
    }

    @Test
    void youCannotCancelSomeoneElsesRequestOrACompletedOne() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        Instant slot = TestSlots.unique();
        RequestView adas = service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, slot);

        assertFails(HttpStatus.NOT_FOUND, () -> service.cancelRequest(grace, adas.id()));

        service.requestInterview(grace, Topic.ALGORITHMS, Level.BEGINNER, slot);
        assertFails(HttpStatus.CONFLICT, () -> service.cancelRequest(ada, adas.id()));
    }

    // -------------------------------------------------------------- slot validation

    @Test
    void slotsMustBeOnTheTwoHourGridAndNotInThePastOrTooFarAway() {
        UUID ada = newUser("Ada");
        Instant good = TestSlots.unique();

        assertFails(HttpStatus.BAD_REQUEST,
                () -> service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, good.plus(Duration.ofHours(1))));
        assertFails(HttpStatus.BAD_REQUEST,
                () -> service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, good.plus(Duration.ofMinutes(30))));
        assertFails(HttpStatus.BAD_REQUEST,
                () -> service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, good.minus(Duration.ofDays(5))));
        assertFails(HttpStatus.BAD_REQUEST,
                () -> service.requestInterview(ada, Topic.ALGORITHMS, Level.BEGINNER, good.plus(Duration.ofDays(30))));
    }

    // -------------------------------------------------------------- roles and secrecy

    @Test
    void theServerNeverSendsHintsOrTheSolutionToThePersonAnswering() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID id = matchedSession(ada, grace, TestSlots.unique());

        SessionView answering = service.getSession(ada, id);
        SessionView interviewing = service.getSession(grace, id);

        assertThat(answering.youAreInterviewee()).isTrue();
        assertThat(answering.question().hints()).isNull();
        assertThat(answering.question().solution()).isNull();
        assertThat(answering.question().prompt()).isNotBlank();
        assertThat(answering.question().starterCode()).contains("def ");

        assertThat(interviewing.youAreInterviewee()).isFalse();
        assertThat(interviewing.question().hints()).isNotEmpty();
        assertThat(interviewing.question().solution()).isNotBlank();
    }

    @Test
    void onlyThePersonAnsweringCanChangeTheCode() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID id = matchedSession(ada, grace, TestSlots.unique());

        service.updateCode(ada, id, "print('hello')");
        assertThat(service.getSession(grace, id).code()).isEqualTo("print('hello')");

        assertFails(HttpStatus.FORBIDDEN, () -> service.updateCode(grace, id, "print('sneaky')"));
        assertThat(service.getSession(ada, id).code()).isEqualTo("print('hello')");
    }

    @Test
    void codeHasASizeLimit() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID id = matchedSession(ada, grace, TestSlots.unique());

        service.updateCode(ada, id, "x".repeat(InterviewService.MAX_CODE_CHARS));
        assertFails(HttpStatus.BAD_REQUEST,
                () -> service.updateCode(ada, id, "x".repeat(InterviewService.MAX_CODE_CHARS + 1)));
    }

    @Test
    void switchingRolesSwapsWhoAnswersAndMovesToTheSecondQuestion() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID id = matchedSession(ada, grace, TestSlots.unique());
        String firstQuestion = service.getSession(ada, id).question().id();
        service.updateCode(ada, id, "work in progress");

        service.switchRoles(grace, id);

        SessionView forAda = service.getSession(ada, id);
        SessionView forGrace = service.getSession(grace, id);
        assertThat(forAda.round()).isEqualTo(2);
        assertThat(forAda.youAreInterviewee()).isFalse();
        assertThat(forGrace.youAreInterviewee()).isTrue();
        assertThat(forAda.question().id()).isNotEqualTo(firstQuestion);
        // The code starts fresh from the new question's starter code, and the secrets flip too.
        assertThat(forGrace.code()).isEqualTo(bank.get(forGrace.question().id()).starterCode());
        assertThat(forGrace.question().hints()).isNull();
        assertThat(forAda.question().hints()).isNotEmpty();

        service.updateCode(grace, id, "grace's answer");
        assertFails(HttpStatus.FORBIDDEN, () -> service.updateCode(ada, id, "no longer allowed"));
        assertFails(HttpStatus.CONFLICT, () -> service.switchRoles(ada, id));
    }

    @Test
    void finishingEndsEditingAndShowsTheSolutionToBothPeople() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID id = matchedSession(ada, grace, TestSlots.unique());
        service.switchRoles(ada, id);

        service.finish(grace, id);

        SessionView forGrace = service.getSession(grace, id);
        assertThat(forGrace.status()).isEqualTo(SessionStatus.DONE);
        assertThat(forGrace.youAreInterviewee()).isTrue();
        assertThat(forGrace.question().solution()).isNotBlank();
        assertFails(HttpStatus.CONFLICT, () -> service.updateCode(grace, id, "too late"));
        assertFails(HttpStatus.CONFLICT, () -> service.finish(ada, id));
    }

    @Test
    void peopleOutsideTheSessionCannotSeeOrChangeIt() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID outsider = newUser("Mallory");
        UUID id = matchedSession(ada, grace, TestSlots.unique());

        assertFails(HttpStatus.NOT_FOUND, () -> service.getSession(outsider, id));
        assertFails(HttpStatus.NOT_FOUND, () -> service.updateCode(outsider, id, "x"));
        assertFails(HttpStatus.NOT_FOUND, () -> service.switchRoles(outsider, id));
        assertFails(HttpStatus.NOT_FOUND, () -> service.finish(outsider, id));
        assertFails(HttpStatus.NOT_FOUND, () -> service.feedbackOverview(outsider, id));
    }

    @Test
    void theSessionListShowsThePartnerAndTheCurrentQuestion() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID id = matchedSession(ada, grace, TestSlots.unique());

        var summaries = service.mySessions(grace);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).id()).isEqualTo(id);
        assertThat(summaries.get(0).partner().displayName()).isEqualTo("Ada");
        assertThat(summaries.get(0).questionTitle()).isNotBlank();
        assertThat(summaries.get(0).feedbackGiven()).isFalse();
    }

    // -------------------------------------------------------------- feedback

    @Test
    void feedbackNeedsAFinishedSessionAndCanBeLeftOnlyOnce() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID id = matchedSession(ada, grace, TestSlots.unique());

        assertFails(HttpStatus.CONFLICT, () -> service.submitFeedback(ada, id, 5, "Great"));

        service.finish(ada, id);
        service.submitFeedback(ada, id, 5, "  Clear thinking out loud.  ");

        assertFails(HttpStatus.CONFLICT, () -> service.submitFeedback(ada, id, 4, "Again"));
        assertThat(service.feedbackOverview(ada, id).given()).isTrue();
        assertThat(service.mySessions(ada).get(0).feedbackGiven()).isTrue();
    }

    @Test
    void youOnlySeeFeedbackYourPartnerWroteAboutYou() {
        UUID ada = newUser("Ada");
        UUID grace = newUser("Grace");
        UUID id = matchedSession(ada, grace, TestSlots.unique());
        service.finish(ada, id);
        service.submitFeedback(ada, id, 4, "Good structure, watch the edge cases.");

        assertThat(service.feedbackOverview(ada, id).received()).isNull();
        var seenByGrace = service.feedbackOverview(grace, id);
        assertThat(seenByGrace.given()).isFalse();
        assertThat(seenByGrace.received().rating()).isEqualTo(4);
        assertThat(seenByGrace.received().comment()).isEqualTo("Good structure, watch the edge cases.");
    }
}
