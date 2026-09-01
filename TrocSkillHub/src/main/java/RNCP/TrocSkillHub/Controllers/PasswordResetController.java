package RNCP.TrocSkillHub.Controllers;

import RNCP.TrocSkillHub.DTOs.PasswordResetDto;
import RNCP.TrocSkillHub.DTOs.PasswordResetRequestDto;
import RNCP.TrocSkillHub.DTOs.PasswordResetVerifyDto;
import RNCP.TrocSkillHub.Config.AuditLogger;
import RNCP.TrocSkillHub.Services.PasswordResetService;
import RNCP.TrocSkillHub.Util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RateLimiter rateLimiter;
    private final AuditLogger auditLogger;
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    public PasswordResetController(
            PasswordResetService passwordResetService,
            RateLimiter rateLimiter,
            AuditLogger auditLogger) {
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
        this.auditLogger = auditLogger;
    }

    @PostMapping("/request")
    public ResponseEntity<?> requestReset(@Valid @RequestBody PasswordResetRequestDto dto) {
        try {
            if (!rateLimiter.isAllowed(dto.getEmail())) {
                logger.warn("Rate limit exceeded for password reset request");
                auditLogger.warning("PWD_RESET_RATE_LIMIT");
                return ResponseEntity.ok().body("If an account with that email exists, a reset code has been sent.");
            }
            logger.info("Processing password reset request");
            passwordResetService.requestReset(dto);
            auditLogger.info("PWD_RESET_REQUEST");
        } catch (Exception e) {
            logger.warn("Error during password reset request: {}", e.getMessage());
            auditLogger.warning("PWD_RESET_REQUEST_ERROR");
        }
        return ResponseEntity.ok().body("If an account with that email exists, a reset code has been sent.");
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody PasswordResetVerifyDto dto) {
        try {
            logger.info("Verifying password reset code");
            String token = passwordResetService.verifyCode(dto);
            logger.info("Password reset code verified successfully");
            auditLogger.info("PWD_RESET_VERIFY_SUCCESS");
            return ResponseEntity.ok().body(token);
        } catch (Exception e) {
            logger.warn("Verification failed: {}", e.getMessage());
            auditLogger.warning("PWD_RESET_VERIFY_FAILED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid verification code or request.");
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetDto dto) {
        try {
            logger.info("Processing password reset");
            passwordResetService.resetPassword(dto);
            logger.info("Password reset successful");
            auditLogger.info("PWD_RESET_SUCCESS");
            return ResponseEntity.ok().body("Password updated successfully.");
        } catch (Exception e) {
            logger.warn("Password reset failed: {}", e.getMessage());
            auditLogger.warning("PWD_RESET_FAILED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to reset password.");
        }
    }
}
