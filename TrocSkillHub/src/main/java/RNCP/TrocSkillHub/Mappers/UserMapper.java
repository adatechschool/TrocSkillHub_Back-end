package RNCP.TrocSkillHub.Mappers;

import java.util.List;

import org.mapstruct.*;

import RNCP.TrocSkillHub.DTOs.UserDTO;
import RNCP.TrocSkillHub.Models.User;

@Mapper(
    componentModel="spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)

public interface UserMapper {
    // Convertit l'entité User en UserDTO
    UserDTO toDTO(User user); 

    // Convertit l'UserDTO en entité User
    User toEntity(UserDTO userDTO);

    // Convertit une liste d'entité User en liste UserDTO
    List<UserDTO> toDTOList(List<User> users);

    // Convertit une liste UserDTO en liste d'entité Users
    List<User> toEntityList(List<UserDTO> userDTOs);
    
} 
