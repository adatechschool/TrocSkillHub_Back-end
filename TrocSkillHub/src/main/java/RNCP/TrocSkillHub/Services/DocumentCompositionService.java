package RNCP.TrocSkillHub.Services;

import RNCP.TrocSkillHub.DTOs.ProfileDocumentDTO;

public interface DocumentCompositionService {

    /**
     * Composes a CV PDF from the profile payload (HTML template → PDF).
     */
    byte[] composePdf(ProfileDocumentDTO profile);
}
