package RNCP.TrocSkillHub.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDTO {
    private Long id;
    @NotBlank
    @Size(max = 120)
    private String name;
    private Long categoryId;
}