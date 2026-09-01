package RNCP.TrocSkillHub.Config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

class AuditFormatterTest {

    @Test
    void formatsInfoLineWithEvent() {
        LogRecord record = new LogRecord(Level.INFO, "AUTH_LOGIN_SUCCESS");

        String line = new AuditFormatter().format(record);

        assertThat(line).contains(" | INFO | AUTH_LOGIN_SUCCESS");
        assertThat(line).endsWith(System.lineSeparator());
    }

    @Test
    void formatsWarningAndSevereLevels() {
        AuditFormatter formatter = new AuditFormatter();

        assertThat(formatter.format(new LogRecord(Level.WARNING, "AUTH_LOGIN_FAILURE")))
                .contains(" | WARNING | AUTH_LOGIN_FAILURE");
        assertThat(formatter.format(new LogRecord(Level.SEVERE, "AUTH_REGISTER_ERROR")))
                .contains(" | SEVERE | AUTH_REGISTER_ERROR");
    }
}
