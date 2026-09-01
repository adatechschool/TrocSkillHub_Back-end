package RNCP.TrocSkillHub.Controllers;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import RNCP.TrocSkillHub.DTOs.UserPublicResponseDTO;
import RNCP.TrocSkillHub.DTOs.UserRequestDTO;
import RNCP.TrocSkillHub.Mappers.UserMapper;
import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Services.UserService;
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @Autowired
    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping
    public ResponseEntity<List<UserPublicResponseDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserPublicResponseDTO> userDTOs = userMapper.toPublicResponseDTOList(users);
        return ResponseEntity.ok(userDTOs);
    }

    /**
     * Authenticated user profile, resolved from the token/session
     * (same approach as /auth/me): never requires the database id in the URL.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        Optional<Long> ownId = resolveOwnId(authentication);
        if (ownId.isEmpty()) {
            return unauthorized();
        }
        return userService.getUserById(ownId.get())
                .map(user -> ResponseEntity.ok(userMapper.toResponseDTO(user)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestBody UserRequestDTO requestDTO, Authentication authentication) {
        Optional<Long> ownId = resolveOwnId(authentication);
        if (ownId.isEmpty()) {
            return unauthorized();
        }
        try {
            User updatedUser = userService.updateUser(ownId.get(), requestDTO);
            return ResponseEntity.ok(userMapper.toResponseDTO(updatedUser));
        } catch (RuntimeException e) {
            return handleUpdateError(e);
        }
    }

    @PatchMapping("/me")
    public ResponseEntity<?> patchCurrentUser(@RequestBody UserRequestDTO requestDTO, Authentication authentication) {
        Optional<Long> ownId = resolveOwnId(authentication);
        if (ownId.isEmpty()) {
            return unauthorized();
        }
        try {
            User updatedUser = userService.patchUser(ownId.get(), requestDTO);
            return ResponseEntity.ok(userMapper.toResponseDTO(updatedUser));
        } catch (RuntimeException e) {
            return handleUpdateError(e);
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteCurrentUser(Authentication authentication) {
        Optional<Long> ownId = resolveOwnId(authentication);
        if (ownId.isEmpty()) {
            return unauthorized();
        }
        try {
            userService.deleteUser(ownId.get());
            return ResponseEntity.ok("Utilisateur supprimé avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Erreur: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequestDTO requestDTO) {
        try {
            User user = userMapper.toEntity(requestDTO);
            User createdUser = userService.createUser(user);
            return new ResponseEntity<>(userMapper.toResponseDTO(createdUser), HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Erreur: " + e.getMessage());
        }
    }
    
    private Optional<Long> resolveOwnId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        return userService.getUserByEmail(authentication.getName()).map(User::getId);
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Erreur: non authentifié");
    }

    private ResponseEntity<?> handleUpdateError(RuntimeException e) {
        String message = e.getMessage() != null ? e.getMessage() : "";
        if (message.contains("existe déjà")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Erreur: " + message);
        }
        if (message.contains("mot de passe")
                || message.contains("Connaissance")
                || message.contains("knowledgeId")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur: " + message);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Erreur: " + message);
    }
}
