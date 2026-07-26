package RNCP.TrocSkillHub.Services.ImplServices;

import RNCP.TrocSkillHub.DTOs.PasswordResetDto;
import RNCP.TrocSkillHub.DTOs.PasswordResetRequestDto;
import RNCP.TrocSkillHub.DTOs.PasswordResetVerifyDto;
import RNCP.TrocSkillHub.Models.PasswordResetRequest;
import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Repositories.PasswordResetRequestRepository;
import RNCP.TrocSkillHub.Repositories.UserRepository;
import RNCP.TrocSkillHub.Services.EmailService;
import RNCP.TrocSkillHub.Services.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int CODE_DIGITS = 6;
    private static final int CODE_BOUND = 1_000_000;

    private final UserRepository userRepository;
    private final PasswordResetRequestRepository resetRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final int codeExpiryMinutes;
    private final int maxAttempts;
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    public PasswordResetServiceImpl(UserRepository userRepository,
            PasswordResetRequestRepository resetRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            @Value("${app.password-reset.code-expiry-minutes:15}") int codeExpiryMinutes,
            @Value("${app.password-reset.max-attempts:5}") int maxAttempts) {
        this.userRepository = userRepository;
        this.resetRepository = resetRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.codeExpiryMinutes = codeExpiryMinutes;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public void requestReset(PasswordResetRequestDto requestDto) {
        Optional<User> userOpt = userRepository.findByEmail(requestDto.getEmail());
        if (userOpt.isEmpty()) {
            logger.info("Password reset request for unknown account");
            return;
        }

        User user = userOpt.get();
        String code = String.format("%0" + CODE_DIGITS + "d", secureRandom.nextInt(CODE_BOUND));
        String codeHash = passwordEncoder.encode(code);
        PasswordResetRequest prr = new PasswordResetRequest();
        prr.setUser(user);
        prr.setCodeHash(codeHash);
        prr.setExpiresAt(LocalDateTime.now().plusMinutes(codeExpiryMinutes));
        prr.setUsed(false);
        prr.setAttempts(0);
        resetRepository.save(prr);
        logger.info("Password reset code generated for user id={}", user.getId());
        emailService.sendPasswordResetCode(user.getEmail(), code);
    }

    @Override
    public String verifyCode(PasswordResetVerifyDto verifyDto) {
        Optional<User> userOpt = userRepository.findByEmail(verifyDto.getEmail());
        if (userOpt.isEmpty()) {
            logger.warn("Code verification for unknown account");
            throw new IllegalArgumentException("Invalid code or request");
        }
        User user = userOpt.get();
        Optional<PasswordResetRequest> prrOpt = resetRepository.findTopByUserOrderByCreatedAtDesc(user);
        if (prrOpt.isEmpty()) {
            logger.warn("Code verification: no reset request found for user id={}", user.getId());
            throw new IllegalArgumentException("Invalid code or request");
        }
        PasswordResetRequest prr = prrOpt.get();
        if (prr.isUsed() || prr.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Code verification failed (expired or used) for request id={}", prr.getId());
            throw new IllegalArgumentException("Code expired or used");
        }
        if (prr.getAttempts() >= maxAttempts) {
            prr.setUsed(true);
            resetRepository.save(prr);
            logger.warn("Code verification blocked: max attempts exceeded for request id={}", prr.getId());
            throw new IllegalArgumentException("Too many attempts");
        }

        boolean matches = passwordEncoder.matches(verifyDto.getCode(), prr.getCodeHash());
        if (!matches) {
            prr.setAttempts(prr.getAttempts() + 1);
            resetRepository.save(prr);
            logger.warn("Invalid code attempt {} for request id={}", prr.getAttempts(), prr.getId());
            throw new IllegalArgumentException("Invalid code or request");
        }

        logger.info("Code verified successfully for request id={}", prr.getId());
        return prr.getId().toString();
    }

    @Override
    public void resetPassword(PasswordResetDto resetDto) {
        Long prrId;
        try {
            prrId = Long.parseLong(resetDto.getResetToken());
        } catch (NumberFormatException e) {
            logger.warn("Invalid reset token format");
            throw new IllegalArgumentException("Invalid reset token");
        }
        Optional<PasswordResetRequest> prrOpt = resetRepository.findById(prrId);
        if (prrOpt.isEmpty()) {
            logger.warn("Reset request not found");
            throw new IllegalArgumentException("Invalid reset token");
        }
        PasswordResetRequest prr = prrOpt.get();
        if (prr.isUsed() || prr.getExpiresAt().isBefore(LocalDateTime.now())) {
            logger.warn("Reset request invalid or expired for id={}", prrId);
            throw new IllegalArgumentException("Reset request invalid or expired");
        }

        if (!resetDto.getNewPassword().equals(resetDto.getConfirmPassword())) {
            logger.warn("Password confirmation mismatch for reset request id={}", prrId);
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (resetDto.getNewPassword().length() < 8) {
            logger.warn("Password does not meet policy for reset request id={}", prrId);
            throw new IllegalArgumentException("Password does not meet policy");
        }

        User user = prr.getUser();
        user.setPassword(passwordEncoder.encode(resetDto.getNewPassword()));
        userRepository.save(user);
        logger.info("Password updated for user id={}", user.getId());

        prr.setUsed(true);
        resetRepository.save(prr);
        logger.info("Reset request marked as used for id={}", prrId);
    }
}
