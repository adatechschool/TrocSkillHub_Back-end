package RNCP.TrocSkillHub.Mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import RNCP.TrocSkillHub.DTOs.KnowledgeDTO;
import RNCP.TrocSkillHub.Models.Knowledge;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface KnowledgeMapper {

    KnowledgeDTO toDTO(Knowledge knowledge);
    
    List<KnowledgeDTO> toDTOList(List<Knowledge> knowledges);
    
    @Mapping(target = "userKnowledge", ignore = true)
    Knowledge toEntity(KnowledgeDTO knowledgeDTO);
}
