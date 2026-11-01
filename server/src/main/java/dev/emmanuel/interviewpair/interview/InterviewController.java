package dev.emmanuel.interviewpair.interview;

import dev.emmanuel.interviewpair.interview.InterviewDtos.FeedbackBody;
import dev.emmanuel.interviewpair.interview.InterviewDtos.FeedbackOverview;
import dev.emmanuel.interviewpair.interview.InterviewDtos.NewRequest;
import dev.emmanuel.interviewpair.interview.InterviewDtos.RequestView;
import dev.emmanuel.interviewpair.interview.InterviewDtos.SessionSummary;
import dev.emmanuel.interviewpair.interview.InterviewDtos.SessionView;
import dev.emmanuel.interviewpair.interview.InterviewDtos.TicketView;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InterviewController {

    private final InterviewService service;
    private final WsTicketService tickets;

    public InterviewController(InterviewService service, WsTicketService tickets) {
        this.service = service;
        this.tickets = tickets;
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @PostMapping("/api/interview-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestView request(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NewRequest body) {
        return service.requestInterview(userId(jwt), body.topic(), body.level(), body.slotStart());
    }

    @GetMapping("/api/interview-requests/mine")
    public List<RequestView> myRequests(@AuthenticationPrincipal Jwt jwt) {
        return service.openRequests(userId(jwt));
    }

    @DeleteMapping("/api/interview-requests/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.cancelRequest(userId(jwt), id);
    }

    @GetMapping("/api/sessions")
    public List<SessionSummary> sessions(@AuthenticationPrincipal Jwt jwt) {
        return service.mySessions(userId(jwt));
    }

    @GetMapping("/api/sessions/{id}")
    public SessionView session(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.getSession(userId(jwt), id);
    }

    @PostMapping("/api/sessions/{id}/switch")
    public SessionView switchRoles(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.switchRoles(userId(jwt), id);
        return service.getSession(userId(jwt), id);
    }

    @PostMapping("/api/sessions/{id}/finish")
    public SessionView finish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.finish(userId(jwt), id);
        return service.getSession(userId(jwt), id);
    }

    @GetMapping("/api/sessions/{id}/feedback")
    public FeedbackOverview feedback(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.feedbackOverview(userId(jwt), id);
    }

    @PostMapping("/api/sessions/{id}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackOverview leaveFeedback(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody FeedbackBody body) {
        service.submitFeedback(userId(jwt), id, body.rating(), body.comment());
        return service.feedbackOverview(userId(jwt), id);
    }

    @PostMapping("/api/sessions/{id}/ws-ticket")
    public TicketView ticket(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        // Checking membership here means a ticket is only ever issued to someone in the session.
        service.getSession(userId(jwt), id);
        return new TicketView(tickets.issue(userId(jwt), id));
    }
}
