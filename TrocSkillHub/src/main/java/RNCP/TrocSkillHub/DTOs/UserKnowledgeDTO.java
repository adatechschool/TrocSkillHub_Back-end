package RNCP.TrocSkillHub.DTOs;

import jakarta.validation.constraints.Size;

public record UserKnowledgeDTO (
    Long knowledgeId,
    @Size(max = 120) String knowledgeName,
    @Size(max = 40) String level,
    @Size(max = 20) String type
  ) {}
