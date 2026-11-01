package dev.emmanuel.interviewpair.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One mock interview between two people. Round one and round two use different questions and the
 * two people swap roles between them. Only the interviewee can change the shared code.
 */
@Entity
@Table(name = "practice_sessions")
public class PracticeSession {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "user_a", nullable = false)
    private UUID userA;

    @Column(name = "user_b", nullable = false)
    private UUID userB;

    @Column(nullable = false)
    private int round;

    @Column(name = "question_one", nullable = false)
    private String questionOne;

    @Column(name = "question_two", nullable = false)
    private String questionTwo;

    @Column(name = "interviewee_id", nullable = false)
    private UUID intervieweeId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PracticeSession() {}

    public PracticeSession(UUID id, Topic topic, Level level, LocalDateTime slotStart, UUID userA, UUID userB,
            Question first, Question second, LocalDateTime createdAt) {
        this.id = id;
        this.topic = topic;
        this.level = level;
        this.slotStart = slotStart;
        this.userA = userA;
        this.userB = userB;
        this.round = 1;
        this.questionOne = first.id();
        this.questionTwo = second.id();
        this.intervieweeId = userA;
        this.code = first.starterCode();
        this.status = SessionStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    public boolean isParticipant(UUID userId) {
        return userA.equals(userId) || userB.equals(userId);
    }

    public UUID partnerOf(UUID userId) {
        return userA.equals(userId) ? userB : userA;
    }

    public String currentQuestionId() {
        return round == 1 ? questionOne : questionTwo;
    }

    /** Swaps the roles and moves to the second question with its fresh starter code. */
    public void startRoundTwo(String starterCode) {
        this.round = 2;
        this.intervieweeId = partnerOf(intervieweeId);
        this.code = starterCode;
    }

    public void finish() {
        this.status = SessionStatus.DONE;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String questionOneId() { return questionOne; }
    public String questionTwoId() { return questionTwo; }
    public UUID getId() { return id; }
    public Topic getTopic() { return topic; }
    public Level getLevel() { return level; }
    public LocalDateTime getSlotStart() { return slotStart; }
    public UUID getUserA() { return userA; }
    public UUID getUserB() { return userB; }
    public int getRound() { return round; }
    public UUID getIntervieweeId() { return intervieweeId; }
    public String getCode() { return code; }
    public SessionStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
