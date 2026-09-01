package RNCP.TrocSkillHub.Config;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Facade over the dedicated JUL audit logger.
 * Never pass email, password, JWT or reset codes as {@code detail}.
 */
public class AuditLogger {

    private final Logger logger;

    AuditLogger(Logger logger) {
        this.logger = logger;
    }

    public void info(String event) {
        log(Level.INFO, event, null);
    }

    public void info(String event, String detail) {
        log(Level.INFO, event, detail);
    }

    public void warning(String event) {
        log(Level.WARNING, event, null);
    }

    public void warning(String event, String detail) {
        log(Level.WARNING, event, detail);
    }

    public void severe(String event) {
        log(Level.SEVERE, event, null);
    }

    public void severe(String event, String detail) {
        log(Level.SEVERE, event, detail);
    }

    private void log(Level level, String event, String detail) {
        String message = (detail == null || detail.isBlank()) ? event : event + " | " + detail;
        logger.log(level, message);
    }
}
