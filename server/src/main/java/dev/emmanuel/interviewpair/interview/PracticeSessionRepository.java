package dev.emmanuel.interviewpair.interview;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PracticeSessionRepository extends JpaRepository<PracticeSession, UUID> {

    @Query("select s from PracticeSession s where s.userA = :user or s.userB = :user order by s.slotStart desc")
    List<PracticeSession> findForUser(@Param("user") UUID user);

    @Query("""
            select count(s) > 0 from PracticeSession s
            where (s.userA = :user or s.userB = :user) and s.slotStart = :slot""")
    boolean existsForUserAtSlot(@Param("user") UUID user, @Param("slot") LocalDateTime slot);
}
