package RNCP.TrocSkillHub.DTOs;

public record ProfileDocumentResponseDTO(
        String status,
        String recipient,
        String documentName,
        String message) {
}
