package RNCP.TrocSkillHub.Mappers;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import RNCP.TrocSkillHub.DTOs.EducationDTO;
import RNCP.TrocSkillHub.DTOs.ExperienceDTO;
import RNCP.TrocSkillHub.DTOs.ProfileDocumentDTO;
import RNCP.TrocSkillHub.DTOs.ProjectDTO;
import RNCP.TrocSkillHub.DTOs.UserKnowledgeDTO;
import RNCP.TrocSkillHub.Models.User;

/**
 * Maps User → ProfileDocumentDTO for document composition.
 * Reuses the skills/needs split from {@link UserMapper}.
 */
@Component
public class ProfileDocumentMapper {

    private final UserMapper userMapper;
    private final String fileNamePrefix;

    public ProfileDocumentMapper(
            UserMapper userMapper,
            @Value("${app.editique.pdf-filename-prefix:profil}") String fileNamePrefix) {
        this.userMapper = userMapper;
        this.fileNamePrefix = fileNamePrefix == null || fileNamePrefix.isBlank()
                ? "profil"
                : fileNamePrefix.trim();
    }

    public ProfileDocumentDTO toDocumentDTO(User user) {
        List<UserKnowledgeDTO> skills = userMapper.mapSkills(user.getUserKnowledge());
        List<UserKnowledgeDTO> needs = userMapper.mapNeeds(user.getUserKnowledge());
        List<EducationDTO> education = user.getEducation() == null
                ? List.of()
                : user.getEducation().stream().map(userMapper::toDTO).toList();
        List<ExperienceDTO> experience = user.getExperience() == null
                ? List.of()
                : user.getExperience().stream().map(userMapper::toDTO).toList();
        List<ProjectDTO> project = user.getProject() == null
                ? List.of()
                : user.getProject().stream().map(userMapper::toDTO).toList();

        String description = user.getDescription();
        boolean hasDescription = description != null && !description.isBlank();

        return new ProfileDocumentDTO(
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getCity(),
                user.getCountry(),
                hasDescription ? description.trim() : null,
                skills,
                needs,
                education,
                experience,
                project,
                !skills.isEmpty(),
                !needs.isEmpty(),
                !education.isEmpty(),
                !experience.isEmpty(),
                !project.isEmpty());
    }

    /**
     * File name: profil-{firstName}-{lastName}.pdf
     * (lowercase, without accents or spaces).
     */
    public String buildFileName(ProfileDocumentDTO profile) {
        String first = normalizeToken(profile.firstName());
        String last = normalizeToken(profile.lastName());
        String base = (first + "-" + last).replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) {
            base = "utilisateur";
        }
        return fileNamePrefix + "-" + base + ".pdf";
    }

    private String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
