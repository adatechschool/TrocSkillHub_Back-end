package RNCP.TrocSkillHub.DTOs;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record ExperienceDTO(
        Long id,
        @Size(max = 120) String company,
        @Size(max = 120) String job,
        LocalDate dateStart,
        LocalDate dateEnd) {
}
