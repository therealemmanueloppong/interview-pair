package dev.emmanuel.interviewpair.interview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InterviewDtos {

    private InterviewDtos() {}

    public record NewRequest(@NotNull Topic topic, @NotNull Level level, @NotNull Instant slotStart) {}

    public record RequestView(UUID id, Topic topic, String topicLabel, Level level, Instant slotStart,
            RequestStatus status, UUID sessionId) {}

    public record PartnerView(UUID id, String displayName) {}

    /** hints and solution are null for the person answering the question. */
    public record QuestionView(String id, String title, String prompt, String language, String starterCode,
            List<String> hints, String solution) {}

    public record SessionSummary(UUID id, Topic topic, String topicLabel, Level level, Instant slotStart,
            SessionStatus status, int round, PartnerView partner, String questionTitle, boolean feedbackGiven) {}

    public record SessionView(UUID id, Topic topic, String topicLabel, Level level, Instant slotStart,
            SessionStatus status, int round, boolean youAreInterviewee, PartnerView partner,
            QuestionView question, String code, boolean feedbackGiven) {}

    public record FeedbackBody(@Min(1) @Max(5) int rating, @NotBlank @Size(max = 1000) String comment) {}

    public record ReceivedFeedback(int rating, String comment, Instant createdAt) {}

    /** What the person gave and what they received. received is null until their partner has written it. */
    public record FeedbackOverview(boolean given, ReceivedFeedback received) {}

    public record TicketView(String ticket) {}
}
