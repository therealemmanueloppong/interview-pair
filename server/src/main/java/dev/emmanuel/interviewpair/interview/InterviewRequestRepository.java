package dev.emmanuel.interviewpair.interview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRequestRepository extends JpaRepository<InterviewRequest, UUID> {

    List<InterviewRequest> findByStatusAndTopicAndLevelAndSlotStart(
            RequestStatus status, Topic topic, Level level, LocalDateTime slotStart);

    List<InterviewRequest> findByUserIdAndStatusOrderBySlotStart(UUID userId, RequestStatus status);

    boolean existsByUserIdAndStatusAndSlotStart(UUID userId, RequestStatus status, LocalDateTime slotStart);
}
