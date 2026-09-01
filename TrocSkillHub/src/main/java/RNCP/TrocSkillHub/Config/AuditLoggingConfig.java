package RNCP.TrocSkillHub.Config;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditLoggingConfig {

    static final String LOGGER_NAME = "trocskillhub.audit";

    @Bean
    public AuditLogger auditLogger() {
        Logger jul = Logger.getLogger(LOGGER_NAME);
        jul.setLevel(Level.INFO);
        jul.setUseParentHandlers(false);
        for (Handler existing : jul.getHandlers()) {
            jul.removeHandler(existing);
        }
        jul.addHandler(new AuditConsoleHandler());
        return new AuditLogger(jul);
    }
}
