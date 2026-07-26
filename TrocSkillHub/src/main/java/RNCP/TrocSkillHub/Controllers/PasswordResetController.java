package RNCP.TrocSkillHub.Controllers;

import RNCP.TrocSkillHub.DTOs.PasswordResetDto;
import RNCP.TrocSkillHub.DTOs.PasswordResetRequestDto;
import RNCP.TrocSkillHub.DTOs.PasswordResetVerifyDto;
import RNCP.TrocSkillHub.Services.PasswordResetService;
import RNCP.TrocSkillHub.Util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;
    private final RateLimiter rateLimiter;
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    public PasswordResetController(PasswordResetService passwordResetService, RateLimiter rateLimiter) {
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/request")
    public ResponseEntity<?> requestReset(@RequestBody PasswordResetRequestDto dto) {
        try {
            if (!rateLimiter.isAllowed(dto.getEmail())) {
                logger.warn("Rate limit exceeded for password reset request");
                return ResponseEntity.ok().body("If an account with that email exists, a reset code has been sent.");
            }
            logger.info("Processing password reset request");
            passwordResetService.requestReset(dto);
        } catch (Exception e) {
            logger.warn("Error during password reset request: {}", e.getMessage());
        }
        return ResponseEntity.ok().body("If an account with that email exists, a reset code has been sent.");
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody PasswordResetVerifyDto dto) {
        try {
            logger.info("Verifying password reset code");
            String token = passwordResetService.verifyCode(dto);
            logger.info("Password reset code verified successfully");
            return ResponseEntity.ok().body(token);
        } catch (Exception e) {
            logger.warn("Verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid verification code or request.");
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetDto dto) {
        try {
            logger.info("Processing password reset");
            passwordResetService.resetPassword(dto);
            logger.info("Password reset successful");
            return ResponseEntity.ok().body("Password updated successfully.");
        } catch (Exception e) {
            logger.warn("Password reset failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unable to reset password.");
        }
    }
}
