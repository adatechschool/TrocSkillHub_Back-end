package RNCP.TrocSkillHub.DTOs;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record ProjectDTO(
        Long id,
        @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @Size(max = 500) String links,
        LocalDate dateStart,
        LocalDate dateEnd) {
}
