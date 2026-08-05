package RNCP.TrocSkillHub.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled support for the whole application. Kept separate from the
 * scheduled beans themselves so that removing one of them cannot silently
 * disable scheduling everywhere.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
