package RNCP.TrocSkillHub.Services.ImplServices;

import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import RNCP.TrocSkillHub.DTOs.ProfileDocumentDTO;
import RNCP.TrocSkillHub.Services.DocumentCompositionService;

/**
 * Simplified CCM composition engine (V1): Thymeleaf + OpenHTMLToPDF.
 */
@Service
public class PdfDocumentCompositionService implements DocumentCompositionService {

    private static final Logger logger = LoggerFactory.getLogger(PdfDocumentCompositionService.class);

    private final TemplateEngine templateEngine;
    private final String templateName;

    public PdfDocumentCompositionService(
            TemplateEngine templateEngine,
            @Value("${app.editique.template:editique/profile-cv}") String templateName) {
        this.templateEngine = templateEngine;
        this.templateName = normalizeTemplateName(templateName);
    }

    @Override
    public byte[] composePdf(ProfileDocumentDTO profile) {
        try {
            Context context = new Context();
            context.setVariable("profile", profile);
            String html = templateEngine.process(templateName, context);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);
                builder.toStream(outputStream);
                builder.run();

                byte[] pdfBytes = outputStream.toByteArray();
                if (pdfBytes.length == 0) {
                    throw new IllegalStateException("Le PDF généré est vide");
                }
                return pdfBytes;
            }
        } catch (RuntimeException e) {
            logger.error("Échec de composition du PDF profil: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Échec de composition du PDF profil: {}", e.getMessage(), e);
            throw new RuntimeException("La composition du PDF a échoué", e);
        }
    }

    /**
     * Accepts either "editique/profile-cv" or a full classpath path.
     */
    private static String normalizeTemplateName(String configured) {
        if (configured == null || configured.isBlank()) {
            return "editique/profile-cv";
        }
        String name = configured.trim();
        if (name.startsWith("classpath:")) {
            name = name.substring("classpath:".length());
        }
        if (name.startsWith("templates/")) {
            name = name.substring("templates/".length());
        }
        if (name.endsWith(".html")) {
            name = name.substring(0, name.length() - ".html".length());
        }
        return name;
    }
}
