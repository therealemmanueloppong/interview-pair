package dev.emmanuel.interviewpair.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_requests")
public class InterviewRequest {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Topic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Level level;

    /** Start of the slot in UTC. */
    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected InterviewRequest() {}

    public InterviewRequest(UUID id, UUID userId, Topic topic, Level level, LocalDateTime slotStart,
            LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.topic = topic;
        this.level = level;
        this.slotStart = slotStart;
        this.status = RequestStatus.OPEN;
        this.createdAt = createdAt;
    }

    public void markMatched(UUID sessionId) {
        this.status = RequestStatus.MATCHED;
        this.sessionId = sessionId;
    }

    public void cancel() {
        this.status = RequestStatus.CANCELLED;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Topic getTopic() { return topic; }
    public Level getLevel() { return level; }
    public LocalDateTime getSlotStart() { return slotStart; }
    public RequestStatus getStatus() { return status; }
    public UUID getSessionId() { return sessionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
