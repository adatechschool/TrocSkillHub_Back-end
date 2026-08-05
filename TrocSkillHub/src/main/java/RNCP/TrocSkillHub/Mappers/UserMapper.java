package RNCP.TrocSkillHub.Mappers;
import java.util.List;
import org.mapstruct.*;
import RNCP.TrocSkillHub.DTOs.EducationDTO;
import RNCP.TrocSkillHub.DTOs.ExperienceDTO;
import RNCP.TrocSkillHub.DTOs.ProjectDTO;
import RNCP.TrocSkillHub.DTOs.UserKnowledgeDTO;
import RNCP.TrocSkillHub.DTOs.UserPublicResponseDTO;
import RNCP.TrocSkillHub.DTOs.UserRequestDTO;
import RNCP.TrocSkillHub.DTOs.UserResponseDTO;
import RNCP.TrocSkillHub.Models.Education;
import RNCP.TrocSkillHub.Models.Experience;
import RNCP.TrocSkillHub.Models.KnowledgeType;
import RNCP.TrocSkillHub.Models.Project;
import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Models.UserKnowledge;
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {
    @Mapping(target = "skills", expression = "java(mapSkills(user.getUserKnowledge()))")
    @Mapping(target = "needs",  expression = "java(mapNeeds(user.getUserKnowledge()))")
    @Mapping(target = "education", source = "education")
    @Mapping(target = "experience", source = "experience")
    @Mapping(target = "project", source = "project")
    UserResponseDTO toResponseDTO(User user);

    @Mapping(target = "skills", expression = "java(mapSkills(user.getUserKnowledge()))")
    @Mapping(target = "needs",  expression = "java(mapNeeds(user.getUserKnowledge()))")
    UserPublicResponseDTO toPublicResponseDTO(User user);
    List<UserPublicResponseDTO> toPublicResponseDTOList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userKnowledge", ignore = true)
    @Mapping(target = "education", ignore = true)
    @Mapping(target = "experience", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "picture", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(UserRequestDTO requestDTO);
    @Mapping(source = "knowledge.id",   target = "knowledgeId")
    @Mapping(source = "knowledge.name", target = "knowledgeName")
    @Mapping(source = "type",           target = "type")
    @Mapping(source = "level",          target = "level")
    UserKnowledgeDTO toUserKnowledgeDTO(UserKnowledge userKnowledge);
    default List<UserKnowledgeDTO> mapSkills(List<UserKnowledge> list) {
        if (list == null) return List.of();
        return list.stream()
            .filter(uk -> uk.getType() == KnowledgeType.SKILL)
            .map(this::toUserKnowledgeDTO)
            .toList();
    }
    default List<UserKnowledgeDTO> mapNeeds(List<UserKnowledge> list) {
        if (list == null) return List.of();
        return list.stream()
            .filter(uk -> uk.getType() == KnowledgeType.NEED)
            .map(this::toUserKnowledgeDTO)
            .toList();
    };

    EducationDTO toDTO(Education education);
    ExperienceDTO toDTO(Experience experience);
    ProjectDTO toDTO(Project project);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Education toEntity(EducationDTO educationDTO);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Experience toEntity(ExperienceDTO experienceDTO);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Project toEntity(ProjectDTO projectDTO);
}
