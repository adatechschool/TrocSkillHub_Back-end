package RNCP.TrocSkillHub.Repositories;

import RNCP.TrocSkillHub.Models.PasswordResetRequest;
import RNCP.TrocSkillHub.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    Optional<PasswordResetRequest> findTopByUserOrderByCreatedAtDesc(User user);

    /**
     * Bulk delete: the expired rows are never read, so they are removed in one
     * statement instead of being loaded as entities first.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetRequest p WHERE p.expiresAt < :time")
    int deleteExpiredBefore(@Param("time") LocalDateTime time);
}
