package RNCP.TrocSkillHub.Config;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * JUL handler that writes INFO / WARNING / SEVERE records to stdout.
 */
public class AuditConsoleHandler extends Handler {

    public AuditConsoleHandler() {
        setLevel(Level.INFO);
        setFormatter(new AuditFormatter());
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        System.out.print(getFormatter().format(record));
    }

    @Override
    public void flush() {
        System.out.flush();
    }

    @Override
    public void close() {
        flush();
    }
}
