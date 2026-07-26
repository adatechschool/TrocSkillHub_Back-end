package RNCP.TrocSkillHub.Services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import RNCP.TrocSkillHub.DTOs.EducationDTO;
import RNCP.TrocSkillHub.DTOs.ExperienceDTO;
import RNCP.TrocSkillHub.DTOs.ProfileDocumentDTO;
import RNCP.TrocSkillHub.DTOs.ProjectDTO;
import RNCP.TrocSkillHub.DTOs.UserKnowledgeDTO;
import RNCP.TrocSkillHub.Services.ImplServices.PdfDocumentCompositionService;

class PdfDocumentCompositionServiceTest {

    private SpringTemplateEngine templateEngine;
    private PdfDocumentCompositionService compositionService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCheckExistence(true);

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        compositionService = new PdfDocumentCompositionService(
                templateEngine,
                "editique/profile-cv");
    }

    @Test
    @DisplayName("composePdf: returns a non-empty PDF for a complete profile")
    void composePdf_completeProfile_returnsNonEmptyPdf() {
        ProfileDocumentDTO profile = completeProfile();

        byte[] pdf = compositionService.composePdf(profile);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("composePdf: returns a non-empty PDF for a minimal profile")
    void composePdf_minimalProfile_returnsNonEmptyPdf() {
        ProfileDocumentDTO profile = minimalProfile();

        byte[] pdf = compositionService.composePdf(profile);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("template: hides Projects section when hasProjects is false")
    void template_withoutProjects_hidesProjectsSection() {
        ProfileDocumentDTO withProjects = completeProfile();
        ProfileDocumentDTO withoutProjects = profileWithoutProjects();

        String htmlWithProjects = renderHtml(withProjects);
        String htmlWithoutProjects = renderHtml(withoutProjects);

        assertThat(htmlWithProjects).contains("Projets");
        assertThat(htmlWithProjects).contains("TrocSkillHub App");
        assertThat(htmlWithoutProjects).doesNotContain("<h2>Projets</h2>");
        assertThat(htmlWithoutProjects).doesNotContain("TrocSkillHub App");
    }

    @Test
    @DisplayName("template: hides empty optional sections")
    void template_minimalProfile_hidesEmptySections() {
        String html = renderHtml(minimalProfile());

        assertThat(html).contains("Fiche compétences");
        assertThat(html).contains("Ada");
        assertThat(html).contains("Lovelace");
        assertThat(html).doesNotContain("<h2>À propos</h2>");
        assertThat(html).doesNotContain("<h2>Compétences proposées</h2>");
        assertThat(html).doesNotContain("<h2>Compétences recherchées</h2>");
        assertThat(html).doesNotContain("<h2>Formations</h2>");
        assertThat(html).doesNotContain("<h2>Expériences</h2>");
        assertThat(html).doesNotContain("<h2>Projets</h2>");
    }

    @Test
    @DisplayName("composePdf: fails when the template cannot be resolved")
    void composePdf_invalidTemplate_throwsRuntimeException() {
        PdfDocumentCompositionService brokenService = new PdfDocumentCompositionService(
                templateEngine,
                "editique/does-not-exist");

        assertThatThrownBy(() -> brokenService.composePdf(minimalProfile()))
                .isInstanceOf(RuntimeException.class);
    }

    private String renderHtml(ProfileDocumentDTO profile) {
        Context context = new Context();
        context.setVariable("profile", profile);
        return templateEngine.process("editique/profile-cv", context);
    }

    private ProfileDocumentDTO completeProfile() {
        return new ProfileDocumentDTO(
                "Jean",
                "Dupont",
                "jean.dupont@test.fr",
                "Paris",
                "France",
                "Développeur full-stack",
                List.of(new UserKnowledgeDTO(1L, "Java", "Avancé", "SKILL")),
                List.of(new UserKnowledgeDTO(2L, "React", "Intermédiaire", "NEED")),
                List.of(new EducationDTO(1L, "Master Info", "Université", null, null)),
                List.of(new ExperienceDTO(1L, "Acme", "Dev", null, null)),
                List.of(new ProjectDTO(1L, "TrocSkillHub App", "Plateforme de troc", "https://example.com", null, null)),
                true,
                true,
                true,
                true,
                true);
    }

    private ProfileDocumentDTO profileWithoutProjects() {
        return new ProfileDocumentDTO(
                "Jean",
                "Dupont",
                "jean.dupont@test.fr",
                "Paris",
                "France",
                "Développeur full-stack",
                List.of(new UserKnowledgeDTO(1L, "Java", "Avancé", "SKILL")),
                List.of(new UserKnowledgeDTO(2L, "React", "Intermédiaire", "NEED")),
                List.of(new EducationDTO(1L, "Master Info", "Université", null, null)),
                List.of(new ExperienceDTO(1L, "Acme", "Dev", null, null)),
                List.of(),
                true,
                true,
                true,
                true,
                false);
    }

    private ProfileDocumentDTO minimalProfile() {
        return new ProfileDocumentDTO(
                "Ada",
                "Lovelace",
                "ada@test.fr",
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                false,
                false,
                false,
                false);
    }
}
