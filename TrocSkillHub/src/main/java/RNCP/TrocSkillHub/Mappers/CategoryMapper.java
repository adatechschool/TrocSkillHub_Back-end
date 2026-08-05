package RNCP.TrocSkillHub.Mappers;


import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import RNCP.TrocSkillHub.DTOs.CategoryDTO;
import RNCP.TrocSkillHub.Models.Category;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryMapper {

    CategoryDTO toDTO(Category category);
    
    List<CategoryDTO> toDTOList(List<Category> categories);
    
    Category toEntity(CategoryDTO categoryDTO);

}
