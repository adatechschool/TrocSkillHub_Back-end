package RNCP.TrocSkillHub.Services;

import org.springframework.security.core.Authentication;

import RNCP.TrocSkillHub.DTOs.ProfileDocumentResponseDTO;

public interface ProfileDocumentOrchestrator {

    ProfileDocumentResponseDTO exportCurrentUserProfile(Authentication authentication);
}
