package RNCP.TrocSkillHub.Services;

import RNCP.TrocSkillHub.Models.User;

import java.io.Long;
import java.util.List;
import java.util.Optional;

public interface UserService {

    // CRUD de base
    User createUser(User user);
    Optional<User> getUserById(Long id);
    List<User> getAllUsers();
    User updateUser(Long id, User user);
    void deleteUser(Long id);
    
    // Méthodes spécifiques basées sur le repository
    Optional<User> getUserByEmail(String email);
    boolean existsByEmail(String email);
    List<User> getUsersByCity(String city);
    List<User> getUsersByCountry(String country);
    
}
