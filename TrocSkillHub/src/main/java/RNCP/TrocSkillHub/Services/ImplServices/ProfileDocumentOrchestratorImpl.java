package RNCP.TrocSkillHub.Services.ImplServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import RNCP.TrocSkillHub.DTOs.ProfileDocumentDTO;
import RNCP.TrocSkillHub.DTOs.ProfileDocumentResponseDTO;
import RNCP.TrocSkillHub.Mappers.ProfileDocumentMapper;
import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Services.DocumentCompositionService;
import RNCP.TrocSkillHub.Services.EmailService;
import RNCP.TrocSkillHub.Services.ProfileDocumentOrchestrator;
import RNCP.TrocSkillHub.Services.UserService;

@Service
public class ProfileDocumentOrchestratorImpl implements ProfileDocumentOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(ProfileDocumentOrchestratorImpl.class);

    private final UserService userService;
    private final ProfileDocumentMapper profileDocumentMapper;
    private final DocumentCompositionService documentCompositionService;
    private final EmailService emailService;

    public ProfileDocumentOrchestratorImpl(
            UserService userService,
            ProfileDocumentMapper profileDocumentMapper,
            DocumentCompositionService documentCompositionService,
            EmailService emailService) {
        this.userService = userService;
        this.profileDocumentMapper = profileDocumentMapper;
        this.documentCompositionService = documentCompositionService;
        this.emailService = emailService;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDocumentResponseDTO exportCurrentUserProfile(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new SecurityException("Non authentifié");
        }

        String email = authentication.getName();
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        ProfileDocumentDTO documentDTO = profileDocumentMapper.toDocumentDTO(user);
        String fileName = profileDocumentMapper.buildFileName(documentDTO);

        byte[] pdfBytes;
        try {
            pdfBytes = documentCompositionService.composePdf(documentDTO);
        } catch (RuntimeException e) {
            logger.error("Échec composition PDF pour user id={}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Échec de la composition du document PDF", e);
        }

        try {
            emailService.sendProfileDocumentPdf(user.getEmail(), pdfBytes, fileName);
        } catch (RuntimeException e) {
            logger.error("Échec envoi email profil pour user id={}: {}", user.getId(), e.getMessage(), e);
            throw new RuntimeException("Échec de l'envoi de l'email avec le document PDF", e);
        }

        return new ProfileDocumentResponseDTO(
                "SENT",
                user.getEmail(),
                fileName,
                "Le CV PDF a été envoyé par email");
    }
}
