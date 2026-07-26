package RNCP.TrocSkillHub.Services.ImplServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import RNCP.TrocSkillHub.Services.EmailService;

@Service
public class SimpleEmailService implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(SimpleEmailService.class);

    private final JavaMailSender mailSender;
    private final String mailFrom;

    public SimpleEmailService(JavaMailSender mailSender,
            @Value("${app.mail.from}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
    }

    @Override
    public void sendPasswordResetCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject("Votre code de réinitialisation TrocSkillHub");
            message.setText("Votre code de réinitialisation est : " + code + "\n\n" +
                    "Ce code expire dans 15 minutes.\n\n" +
                    "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.");
            mailSender.send(message);
            logger.info("Password reset code email sent");
        } catch (Exception e) {
            logger.error("Failed to send password reset code email: {}", e.getMessage());
        }
    }

    @Override
    public void sendProfileDocumentPdf(String to, byte[] pdfContent, String fileName) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject("Votre fiche profil TrocSkillHub");
            helper.setText(
                    "Bonjour,\n\n"
                            + "Votre fiche de compétences (CV PDF) a bien été générée.\n"
                            + "Vous la trouverez en pièce jointe de cet email.\n\n"
                            + "L'équipe TrocSkillHub");

            String attachmentName = (fileName != null && !fileName.isBlank())
                    ? fileName
                    : "profil.pdf";
            helper.addAttachment(attachmentName, new ByteArrayResource(pdfContent), "application/pdf");

            mailSender.send(mimeMessage);
            logger.info("Profile document PDF email sent");
        } catch (Exception e) {
            logger.error("Failed to send profile document PDF email: {}", e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email avec le document PDF", e);
        }
    }
}
