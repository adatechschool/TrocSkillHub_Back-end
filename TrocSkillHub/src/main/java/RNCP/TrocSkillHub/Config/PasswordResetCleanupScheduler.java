package RNCP.TrocSkillHub.Config;

import RNCP.TrocSkillHub.Repositories.PasswordResetRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PasswordResetCleanupScheduler {

    private final PasswordResetRequestRepository resetRepository;
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetCleanupScheduler.class);

    public PasswordResetCleanupScheduler(PasswordResetRequestRepository resetRepository) {
        this.resetRepository = resetRepository;
    }

    /**
     * Deletes expired password reset requests every hour, on the hour.
     *
     * Expired requests are already rejected by the service, so this is not an
     * access control: it limits how long a hash of a six-digit code stays in the
     * database, and therefore what a database leak would expose.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredResetRequests() {
        try {
            int deleted = resetRepository.deleteExpiredBefore(LocalDateTime.now());
            logger.info("Deleted {} expired password reset requests.", deleted);
        } catch (Exception e) {
            logger.error("Error during cleanup of password reset requests: {}", e.getMessage(), e);
        }
    }
}
