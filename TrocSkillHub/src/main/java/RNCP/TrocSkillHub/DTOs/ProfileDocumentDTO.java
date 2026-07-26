package RNCP.TrocSkillHub.DTOs;

import java.util.List;

/**
 * CV PDF composition payload (no password, no raw picture).
 */
public record ProfileDocumentDTO(
        String firstName,
        String lastName,
        String email,
        String city,
        String country,
        String description,
        List<UserKnowledgeDTO> skills,
        List<UserKnowledgeDTO> needs,
        List<EducationDTO> education,
        List<ExperienceDTO> experience,
        List<ProjectDTO> project,
        boolean hasSkills,
        boolean hasNeeds,
        boolean hasEducation,
        boolean hasExperience,
        boolean hasProjects) {
}
