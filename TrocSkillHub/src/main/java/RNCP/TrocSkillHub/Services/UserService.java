package RNCP.TrocSkillHub.Services;

import RNCP.TrocSkillHub.Models.User;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

public interface UserService {

    // CRUD de base
    User createUser(User user);
    Optional<User> getUserById(Serial id);
    List<User> getAllUsers();
    User updateUser(Serial id, User user);
    void deleteUser(Serial id);
    
    // Méthodes spécifiques basées sur le repository
    Optional<User> getUserByEmail(String email);
    boolean existsByEmail(String email);
    List<User> getUsersByCity(String city);
    List<User> getUsersByCountry(String country);
    
}
