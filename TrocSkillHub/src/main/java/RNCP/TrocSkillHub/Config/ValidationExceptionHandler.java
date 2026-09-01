package RNCP.TrocSkillHub.Config;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ValidationExceptionHandler {

    private final AuditLogger auditLogger;

    public ValidationExceptionHandler(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidBody(MethodArgumentNotValidException ex) {
        auditLogger.warning("VALIDATION_FAILED");
        List<Map<String, String>> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalide"))
                .toList();
        return ResponseEntity.badRequest().body(Map.of(
                "error", "Données invalides",
                "details", details));
    }
}
