package dev.emmanuel.interviewpair.interview;

import dev.emmanuel.interviewpair.interview.InterviewDtos.FeedbackOverview;
import dev.emmanuel.interviewpair.interview.InterviewDtos.PartnerView;
import dev.emmanuel.interviewpair.interview.InterviewDtos.QuestionView;
import dev.emmanuel.interviewpair.interview.InterviewDtos.ReceivedFeedback;
import dev.emmanuel.interviewpair.interview.InterviewDtos.RequestView;
import dev.emmanuel.interviewpair.interview.InterviewDtos.SessionSummary;
import dev.emmanuel.interviewpair.interview.InterviewDtos.SessionView;
import dev.emmanuel.interviewpair.matching.MatchRequest;
import dev.emmanuel.interviewpair.matching.MatchingService;
import dev.emmanuel.interviewpair.user.User;
import dev.emmanuel.interviewpair.user.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The rules of a mock interview: how a request becomes a session, who may see and change what, and
 * how the two roles swap. Slots are two hours long on the UTC grid so everyone sees the same times
 * whatever their time zone, and a session is planned for one hour inside its slot.
 */
@Service
public class InterviewService {

    static final Duration SESSION_LENGTH = Duration.ofHours(1);
    static final Duration MAX_AHEAD = Duration.ofDays(7);
    static final int MAX_CODE_CHARS = 20_000;

    private final InterviewRequestRepository requests;
    private final PracticeSessionRepository sessions;
    private final SessionFeedbackRepository feedback;
    private final UserRepository users;
    private final MatchingService matching;
    private final QuestionBank bank;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    // Not RandomGenerator.getDefault(): the slim JRE in the Docker image has no generator for it.
    private final RandomGenerator random = new Random();

    // Two people asking at the same moment must not both miss each other, so matching runs one at a
    // time. This is enough for a single server instance. More instances would need a database lock.
    private final Object matchLock = new Object();

    public InterviewService(InterviewRequestRepository requests, PracticeSessionRepository sessions,
            SessionFeedbackRepository feedback, UserRepository users, MatchingService matching, QuestionBank bank,
            Clock clock, ApplicationEventPublisher events, TransactionTemplate transaction) {
        this.requests = requests;
        this.sessions = sessions;
        this.feedback = feedback;
        this.users = users;
        this.matching = matching;
        this.bank = bank;
        this.clock = clock;
        this.events = events;
        this.transaction = transaction;
    }

    // ------------------------------------------------------------------ requests

    public RequestView requestInterview(UUID userId, Topic topic, Level level, Instant slotStart) {
        validateSlot(slotStart);
        LocalDateTime slot = LocalDateTime.ofInstant(slotStart, ZoneOffset.UTC);
        synchronized (matchLock) {
            return transaction.execute(status -> createAndMatch(userId, topic, level, slot));
        }
    }

    private RequestView createAndMatch(UUID userId, Topic topic, Level level, LocalDateTime slot) {
        if (requests.existsByUserIdAndStatusAndSlotStart(userId, RequestStatus.OPEN, slot)
                || sessions.existsForUserAtSlot(userId, slot)) {
            throw new InterviewException(HttpStatus.CONFLICT, "You already have an interview in that time slot");
        }
        InterviewRequest mine = requests.save(
                new InterviewRequest(UUID.randomUUID(), userId, topic, level, slot, LocalDateTime.now(clock)));

        List<InterviewRequest> pool =
                requests.findByStatusAndTopicAndLevelAndSlotStart(RequestStatus.OPEN, topic, level, slot);
        Optional<MatchRequest> partner = matching.findPartner(
                toMatchRequest(mine), pool.stream().map(InterviewService::toMatchRequest).toList());

        if (partner.isPresent()) {
            InterviewRequest theirs = pool.stream()
                    .filter(r -> r.getUserId().equals(partner.get().userId()))
                    .findFirst()
                    .orElseThrow();
            List<Question> picked = bank.pickTwo(topic, level, random);
            // The person who asked first answers first.
            PracticeSession session = sessions.save(new PracticeSession(UUID.randomUUID(), topic, level, slot,
                    theirs.getUserId(), userId, picked.get(0), picked.get(1), LocalDateTime.now(clock)));
            theirs.markMatched(session.getId());
            mine.markMatched(session.getId());
            requests.saveAll(List.of(theirs, mine));
        }
        return requestView(mine);
    }

    private static MatchRequest toMatchRequest(InterviewRequest r) {
        return new MatchRequest(r.getUserId(), r.getTopic().name(), r.getLevel().name(), r.getSlotStart(),
                r.getSlotStart().plus(SESSION_LENGTH));
    }

    @Transactional(readOnly = true)
    public List<RequestView> openRequests(UUID userId) {
        return requests.findByUserIdAndStatusOrderBySlotStart(userId, RequestStatus.OPEN).stream()
                .map(InterviewService::requestView)
                .toList();
    }

    @Transactional
    public void cancelRequest(UUID userId, UUID requestId) {
        InterviewRequest request = requests.findById(requestId)
                .filter(r -> r.getUserId().equals(userId))
                .orElseThrow(() -> new InterviewException(HttpStatus.NOT_FOUND, "Request not found"));
        if (request.getStatus() != RequestStatus.OPEN) {
            throw new InterviewException(HttpStatus.CONFLICT, "This request is no longer waiting for a partner");
        }
        request.cancel();
    }

    void validateSlot(Instant slotStart) {
        ZonedDateTime utc = slotStart.atZone(ZoneOffset.UTC);
        boolean onGrid = utc.getHour() % 2 == 0 && utc.getMinute() == 0 && utc.getSecond() == 0 && utc.getNano() == 0;
        if (!onGrid) {
            throw new InterviewException(HttpStatus.BAD_REQUEST, "Choose one of the listed time slots");
        }
        Instant now = clock.instant();
        if (!slotStart.plus(SESSION_LENGTH).isAfter(now)) {
            throw new InterviewException(HttpStatus.BAD_REQUEST, "That time slot has already passed");
        }
        if (slotStart.isAfter(now.plus(MAX_AHEAD))) {
            throw new InterviewException(HttpStatus.BAD_REQUEST, "Slots can be booked up to 7 days ahead");
        }
    }

    // ------------------------------------------------------------------ sessions

    @Transactional(readOnly = true)
    public List<SessionSummary> mySessions(UUID userId) {
        List<PracticeSession> mine = sessions.findForUser(userId);
        Set<UUID> partnerIds = new HashSet<>();
        mine.forEach(s -> partnerIds.add(s.partnerOf(userId)));
        Map<UUID, User> partners = new HashMap<>();
        users.findAllById(partnerIds).forEach(u -> partners.put(u.getId(), u));
        Set<UUID> given = new HashSet<>();
        if (!mine.isEmpty()) {
            feedback.findByFromUserAndSessionIdIn(userId, mine.stream().map(PracticeSession::getId).toList())
                    .forEach(f -> given.add(f.getSessionId()));
        }
        return mine.stream()
                .map(s -> new SessionSummary(s.getId(), s.getTopic(), s.getTopic().label(), s.getLevel(),
                        instant(s.getSlotStart()), s.getStatus(), s.getRound(),
                        partnerView(partners.get(s.partnerOf(userId)), s.partnerOf(userId)),
                        bank.get(s.currentQuestionId()).title(), given.contains(s.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionView getSession(UUID userId, UUID sessionId) {
        return view(requireParticipant(userId, sessionId), userId);
    }

    @Transactional
    public void updateCode(UUID userId, UUID sessionId, String text) {
        PracticeSession session = requireParticipant(userId, sessionId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InterviewException(HttpStatus.CONFLICT, "This session has ended");
        }
        if (!session.getIntervieweeId().equals(userId)) {
            throw new InterviewException(HttpStatus.FORBIDDEN, "Only the person answering can edit the code");
        }
        if (text.length() > MAX_CODE_CHARS) {
            throw new InterviewException(HttpStatus.BAD_REQUEST, "That is too much text for one session");
        }
        session.setCode(text);
    }

    @Transactional
    public void switchRoles(UUID userId, UUID sessionId) {
        PracticeSession session = requireParticipant(userId, sessionId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InterviewException(HttpStatus.CONFLICT, "This session has ended");
        }
        if (session.getRound() != 1) {
            throw new InterviewException(HttpStatus.CONFLICT, "The roles have already been switched");
        }
        session.startRoundTwo(bank.get(session.questionTwoId()).starterCode());
        events.publishEvent(new SessionChanged(sessionId));
    }

    @Transactional
    public void finish(UUID userId, UUID sessionId) {
        PracticeSession session = requireParticipant(userId, sessionId);
        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new InterviewException(HttpStatus.CONFLICT, "This session has already ended");
        }
        session.finish();
        events.publishEvent(new SessionChanged(sessionId));
    }

    // ------------------------------------------------------------------ feedback

    @Transactional
    public void submitFeedback(UUID userId, UUID sessionId, int rating, String comment) {
        PracticeSession session = requireParticipant(userId, sessionId);
        if (session.getStatus() != SessionStatus.DONE) {
            throw new InterviewException(HttpStatus.CONFLICT, "Finish the session before leaving feedback");
        }
        if (feedback.existsBySessionIdAndFromUser(sessionId, userId)) {
            throw new InterviewException(HttpStatus.CONFLICT, "You already left feedback for this session");
        }
        feedback.save(new SessionFeedback(UUID.randomUUID(), sessionId, userId, session.partnerOf(userId), rating,
                comment.trim(), LocalDateTime.now(clock)));
    }

    @Transactional(readOnly = true)
    public FeedbackOverview feedbackOverview(UUID userId, UUID sessionId) {
        requireParticipant(userId, sessionId);
        ReceivedFeedback received = feedback.findBySessionIdAndToUser(sessionId, userId)
                .map(f -> new ReceivedFeedback(f.getRating(), f.getComment(), instant(f.getCreatedAt())))
                .orElse(null);
        return new FeedbackOverview(feedback.existsBySessionIdAndFromUser(sessionId, userId), received);
    }

    // ------------------------------------------------------------------ helpers

    private PracticeSession requireParticipant(UUID userId, UUID sessionId) {
        // A session that is not yours looks the same as one that does not exist.
        return sessions.findById(sessionId)
                .filter(s -> s.isParticipant(userId))
                .orElseThrow(() -> new InterviewException(HttpStatus.NOT_FOUND, "Session not found"));
    }

    private SessionView view(PracticeSession s, UUID viewer) {
        Question q = bank.get(s.currentQuestionId());
        // The interviewer needs the hints and solution. The person answering must not see them
        // until the session is over, so the server never sends them early.
        boolean reveal = s.getStatus() == SessionStatus.DONE || !s.getIntervieweeId().equals(viewer);
        QuestionView question = new QuestionView(q.id(), q.title(), q.prompt(), q.topic().language(),
                q.starterCode(), reveal ? q.hints() : null, reveal ? q.solution() : null);
        UUID partnerId = s.partnerOf(viewer);
        PartnerView partner = partnerView(users.findById(partnerId).orElse(null), partnerId);
        return new SessionView(s.getId(), s.getTopic(), s.getTopic().label(), s.getLevel(), instant(s.getSlotStart()),
                s.getStatus(), s.getRound(), s.getIntervieweeId().equals(viewer), partner, question, s.getCode(),
                feedback.existsBySessionIdAndFromUser(s.getId(), viewer));
    }

    private static PartnerView partnerView(User user, UUID fallbackId) {
        return new PartnerView(fallbackId, user == null ? "Former member" : user.getDisplayName());
    }

    private static RequestView requestView(InterviewRequest r) {
        return new RequestView(r.getId(), r.getTopic(), r.getTopic().label(), r.getLevel(), instant(r.getSlotStart()),
                r.getStatus(), r.getSessionId());
    }

    private static Instant instant(LocalDateTime utc) {
        return utc.toInstant(ZoneOffset.UTC);
    }
}
