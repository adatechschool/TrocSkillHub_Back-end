package RNCP.TrocSkillHub.Services;

public interface EmailService {
    void sendPasswordResetCode(String to, String code);

    /**
     * Sends the profile document (PDF) as an email attachment.
     * Throws a RuntimeException on failure.
     */
    void sendProfileDocumentPdf(String to, byte[] pdfContent, String fileName);
}
