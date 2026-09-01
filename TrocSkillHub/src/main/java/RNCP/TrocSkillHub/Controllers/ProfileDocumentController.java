package RNCP.TrocSkillHub.Controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import RNCP.TrocSkillHub.DTOs.ProfileDocumentResponseDTO;
import RNCP.TrocSkillHub.Services.ProfileDocumentOrchestrator;

@RestController
@RequestMapping("/users")
public class ProfileDocumentController {

    private final ProfileDocumentOrchestrator profileDocumentOrchestrator;

    public ProfileDocumentController(ProfileDocumentOrchestrator profileDocumentOrchestrator) {
        this.profileDocumentOrchestrator = profileDocumentOrchestrator;
    }

    /**
     * Composes the authenticated user's CV as a PDF and sends it by email.
     */
    @PostMapping("/me/profile-document")
    public ResponseEntity<?> exportProfileDocument(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorBody("Non authentifié"));
        }

        try {
            ProfileDocumentResponseDTO response =
                    profileDocumentOrchestrator.exportCurrentUserProfile(authentication);
            return ResponseEntity.accepted().body(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(errorBody(e.getMessage() != null ? e.getMessage() : "Non authentifié"));
        } catch (RuntimeException e) {
            String message = e.getMessage() != null
                    ? e.getMessage()
                    : "Une erreur est survenue lors de l'export du profil";
            if (message.toLowerCase().contains("non trouvé")
                    || message.toLowerCase().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(errorBody(message));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody(message));
        }
    }

    private ProfileDocumentResponseDTO errorBody(String message) {
        return new ProfileDocumentResponseDTO("FAILED", null, null, message);
    }
}
