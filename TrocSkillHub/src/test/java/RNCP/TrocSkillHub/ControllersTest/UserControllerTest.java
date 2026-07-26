package RNCP.TrocSkillHub.ControllersTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import RNCP.TrocSkillHub.Controllers.UserController;
import RNCP.TrocSkillHub.DTOs.UserPublicResponseDTO;
import RNCP.TrocSkillHub.DTOs.UserRequestDTO;
import RNCP.TrocSkillHub.DTOs.UserResponseDTO;
import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Mappers.UserMapper;
import RNCP.TrocSkillHub.Services.UserService;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private User user;
    private UserRequestDTO userRequestDTO;
    private UserResponseDTO userResponseDTO;
    private UserPublicResponseDTO userPublicResponseDTO;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        userRequestDTO = new UserRequestDTO(
            "John", "Doe",
            "test@example.com",
            null, null, null, null, null,
            null, null, null, null, null,
            null
        );
        userResponseDTO = new UserResponseDTO(
            "jean", "ali", "test@example.com",
            "test",
         null, null, null,
            null, null, null, null, null,null
        );
        userPublicResponseDTO = new UserPublicResponseDTO(
            "jean", "ali", null, null, null, null
        );

        // By default, the authenticated user is the owner (id=1);
        // used only by tests that need it (lenient to avoid
        // UnnecessaryStubbingException on tests that do not).
        lenient().when(authentication.getName()).thenReturn("test@example.com");
        lenient().when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("getAllUsers - Should return a public list of users, without sensitive data")
    void getAllUsers_shouldReturnUserList() {
        List<User> users = Arrays.asList(user);
        List<UserPublicResponseDTO> responseDTOs = Arrays.asList(userPublicResponseDTO);

        when(userService.getAllUsers()).thenReturn(users);
        when(userMapper.toPublicResponseDTOList(users)).thenReturn(responseDTOs);

        ResponseEntity<List<UserPublicResponseDTO>> response = userController.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).firstName()).isEqualTo("jean");
        assertThat(response.getBody().get(0).lastName()).isEqualTo("ali");
    }

    @Test
    @DisplayName("getAllUsers - should return empty list when no users")
    void getAllUsers_shouldReturnEmptyList_whenNoUsers() {
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());
        when(userMapper.toPublicResponseDTOList(Collections.emptyList())).thenReturn(Collections.emptyList());

        ResponseEntity<List<UserPublicResponseDTO>> response = userController.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @DisplayName("getCurrentUser - should return the authenticated user's own profile")
    void getCurrentUser_shouldReturnUser_whenUserExists() {
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        ResponseEntity<?> response = userController.getCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userResponseDTO);
    }

    @Test
    @DisplayName("getCurrentUser - should return 404 when user Not Found")
    void getCurrentUser_shouldReturn404_whenUserNotFound() {
        User self99 = new User();
        self99.setId(99L);
        self99.setEmail("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(self99));
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = userController.getCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("getCurrentUser - should return 401 when not authenticated")
    void getCurrentUser_shouldReturn401_whenNotAuthenticated() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = userController.getCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("createUser - should create user successfully")
    void createUser_shouldCreateUser_successfully() {
        when(userMapper.toEntity(userRequestDTO)).thenReturn(user);
        when(userService.createUser(user)).thenReturn(user);
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        ResponseEntity<?> response = userController.createUser(userRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(userResponseDTO);
    }

    @Test
    @DisplayName("createUser - should return 409 when Conflict")
    void createUser_shouldReturn409_whenConflict() {
        when(userMapper.toEntity(userRequestDTO)).thenReturn(user);
        when(userService.createUser(user)).thenThrow(new RuntimeException("Email déjà utilisé"));

        ResponseEntity<?> response = userController.createUser(userRequestDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().toString()).contains("Erreur");
    }

    @Test
    @DisplayName("updateCurrentUser - should update the authenticated user's own profile")
    void updateCurrentUser_shouldUpdateUser_successfully() {
        when(userService.updateUser(eq(1L), eq(userRequestDTO))).thenReturn(user);
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        ResponseEntity<?> response = userController.updateCurrentUser(userRequestDTO, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userResponseDTO);
    }

    @Test
    @DisplayName("updateCurrentUser - should return 404 when user Not Found")
    void updateCurrentUser_shouldReturn404_whenUserNotFound() {
        User self99 = new User();
        self99.setId(99L);
        self99.setEmail("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(self99));
        when(userService.updateUser(eq(99L), eq(userRequestDTO)))
                .thenThrow(new RuntimeException("Utilisateur non trouvé"));

        ResponseEntity<?> response = userController.updateCurrentUser(userRequestDTO, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("updateCurrentUser - should return 401 when not authenticated")
    void updateCurrentUser_shouldReturn401_whenNotAuthenticated() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = userController.updateCurrentUser(userRequestDTO, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("patchCurrentUser - should patch the authenticated user's own profile")
    void patchCurrentUser_shouldPatchUser_successfully() {
        when(userService.patchUser(eq(1L), eq(userRequestDTO))).thenReturn(user);
        when(userMapper.toResponseDTO(user)).thenReturn(userResponseDTO);

        ResponseEntity<?> response = userController.patchCurrentUser(userRequestDTO, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(userResponseDTO);
    }

    @Test
    @DisplayName("patchCurrentUser - should return 404 when user Not Found")
    void patchCurrentUser_shouldReturn404_whenUserNotFound() {
        User self99 = new User();
        self99.setId(99L);
        self99.setEmail("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(self99));
        when(userService.patchUser(eq(99L), eq(userRequestDTO)))
                .thenThrow(new RuntimeException("Utilisateur non trouvé"));

        ResponseEntity<?> response = userController.patchCurrentUser(userRequestDTO, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("patchCurrentUser - should return 401 when not authenticated")
    void patchCurrentUser_shouldReturn401_whenNotAuthenticated() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = userController.patchCurrentUser(userRequestDTO, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("deleteCurrentUser - should delete the authenticated user's own profile")
    void deleteCurrentUser_shouldDeleteUser_successfully() {
        doNothing().when(userService).deleteUser(1L);

        ResponseEntity<?> response = userController.deleteCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().toString()).contains("supprimé");
    }

    @Test
    @DisplayName("deleteCurrentUser - should return 404 when user Not Found")
    void deleteCurrentUser_shouldReturn404_whenUserNotFound() {
        User self99 = new User();
        self99.setId(99L);
        self99.setEmail("test@example.com");
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(self99));
        doThrow(new RuntimeException("Utilisateur non trouvé"))
                .when(userService).deleteUser(99L);

        ResponseEntity<?> response = userController.deleteCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("deleteCurrentUser - should return 401 when not authenticated")
    void deleteCurrentUser_shouldReturn401_whenNotAuthenticated() {
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = userController.deleteCurrentUser(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
