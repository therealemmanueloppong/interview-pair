package dev.emmanuel.interviewpair.interview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_feedback")
public class SessionFeedback {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "from_user", nullable = false)
    private UUID fromUser;

    @Column(name = "to_user", nullable = false)
    private UUID toUser;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SessionFeedback() {}

    public SessionFeedback(UUID id, UUID sessionId, UUID fromUser, UUID toUser, int rating, String comment,
            LocalDateTime createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public UUID getFromUser() { return fromUser; }
    public UUID getToUser() { return toUser; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
