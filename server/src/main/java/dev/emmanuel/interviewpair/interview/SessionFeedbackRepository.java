package dev.emmanuel.interviewpair.interview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, UUID> {

    boolean existsBySessionIdAndFromUser(UUID sessionId, UUID fromUser);

    Optional<SessionFeedback> findBySessionIdAndToUser(UUID sessionId, UUID toUser);

    List<SessionFeedback> findByFromUserAndSessionIdIn(UUID fromUser, List<UUID> sessionIds);
}
