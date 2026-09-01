package RNCP.TrocSkillHub.DTOs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
    @Size(max = 80) String firstName,
    @Size(max = 80) String lastName,
    @Email @Size(max = 120) String email,
    @Size(max = 200) String address,
    @Size(max = 80) String city,
    @Size(max = 80) String country,
    @Size(max = 30) String phoneNumber,
    @Size(max = 2000) String description,
    @Valid List<UserKnowledgeDTO> skills,
    @Valid List<UserKnowledgeDTO> needs,
    @Valid List<EducationDTO> education,
    @Valid List<ExperienceDTO> experience,
    @Valid @JsonAlias("projet") List<ProjectDTO> project,
    @Size(min = 8, max = 72) String password
) {}
