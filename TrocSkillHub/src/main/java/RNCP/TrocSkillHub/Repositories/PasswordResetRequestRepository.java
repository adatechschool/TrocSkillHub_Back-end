package RNCP.TrocSkillHub.Repositories;

import RNCP.TrocSkillHub.Models.PasswordResetRequest;
import RNCP.TrocSkillHub.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    Optional<PasswordResetRequest> findTopByUserOrderByCreatedAtDesc(User user);

    @Query("SELECT p FROM PasswordResetRequest p WHERE p.expiresAt < :time")
    List<PasswordResetRequest> findByExpiresAtBefore(@Param("time") LocalDateTime time);
}
