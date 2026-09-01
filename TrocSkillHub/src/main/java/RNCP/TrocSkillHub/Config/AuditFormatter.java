package RNCP.TrocSkillHub.Config;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * JUL formatter: {@code timestamp | LEVEL | event [| detail]}.
 */
public class AuditFormatter extends Formatter {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    @Override
    public String format(LogRecord record) {
        String timestamp = ISO_INSTANT.format(Instant.ofEpochMilli(record.getMillis()));
        String level = record.getLevel().getName();
        String message = formatMessage(record);
        return timestamp + " | " + level + " | " + message + System.lineSeparator();
    }
}
