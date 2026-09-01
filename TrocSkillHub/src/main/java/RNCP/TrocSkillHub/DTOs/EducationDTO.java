package RNCP.TrocSkillHub.DTOs;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record EducationDTO(
        Long id,
        @Size(max = 120) String name,
        @Size(max = 120) String school,
        LocalDate dateStart,
        LocalDate dateEnd) {
}
