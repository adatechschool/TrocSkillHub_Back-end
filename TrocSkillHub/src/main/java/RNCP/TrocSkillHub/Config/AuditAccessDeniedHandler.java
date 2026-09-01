package RNCP.TrocSkillHub.Config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditAccessDeniedHandler implements AccessDeniedHandler {

    private final AuditLogger auditLogger;

    public AuditAccessDeniedHandler(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        if (ex instanceof CsrfException) {
            auditLogger.warning("AUTH_CSRF_REJECTED");
        } else {
            auditLogger.warning("AUTH_ACCESS_DENIED");
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\":\"Accès refusé\"}");
    }
}
