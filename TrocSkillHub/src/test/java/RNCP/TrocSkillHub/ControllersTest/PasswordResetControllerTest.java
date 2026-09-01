package RNCP.TrocSkillHub.ControllersTest;

import RNCP.TrocSkillHub.DTOs.PasswordResetDto;
import RNCP.TrocSkillHub.DTOs.PasswordResetRequestDto;
import RNCP.TrocSkillHub.DTOs.PasswordResetVerifyDto;
import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class PasswordResetControllerTest {

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("trocskillhubdb_test");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("InitialPassword123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setFirstName("testuser");
        // testUser.setRole("USER");
        userRepository.save(testUser);
    }

    @Test
    @WithAnonymousUser
    void testRequestReset_Success() throws Exception {
        PasswordResetRequestDto dto = new PasswordResetRequestDto("test@example.com");

        MvcResult result = mockMvc.perform(post("/auth/password-reset/request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertThat(response).isEqualTo("If an account with that email exists, a reset code has been sent.");
        assertThat(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void testRequestReset_UnknownEmail() throws Exception {
        PasswordResetRequestDto dto = new PasswordResetRequestDto("unknown@example.com");

        mockMvc.perform(post("/auth/password-reset/request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("If an account with that email exists, a reset code has been sent."));
    }

    @Test
    @WithAnonymousUser
    void testRequestReset_RateLimited() throws Exception {
        PasswordResetRequestDto dto = new PasswordResetRequestDto("test@example.com");

        // Do 3 request 
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/password-reset/request")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk());
        }

        // rate limited
        mockMvc.perform(post("/auth/password-reset/request")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("If an account with that email exists, a reset code has been sent."));
    }

    @Test
    @WithAnonymousUser
    void testVerifyCode_InvalidCode() throws Exception {
        PasswordResetVerifyDto dto = new PasswordResetVerifyDto("test@example.com", "wrong");

        mockMvc.perform(post("/auth/password-reset/verify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Invalid verification code or request."));
    }

    @Test
    @WithAnonymousUser
    void testVerifyCode_UnknownEmail() throws Exception {
        PasswordResetVerifyDto dto = new PasswordResetVerifyDto("unknown@example.com", "1234");

        mockMvc.perform(post("/auth/password-reset/verify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Invalid verification code or request."));
    }

    @Test
    @WithAnonymousUser
    void testResetPassword_InvalidToken() throws Exception {
        PasswordResetDto dto = new PasswordResetDto("invalid_token", "NewPassword123", "NewPassword123");

        mockMvc.perform(post("/auth/password-reset/reset")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Unable to reset password."));
    }

    @Test
    @WithAnonymousUser
    void testResetPassword_PasswordMismatch() throws Exception {
        PasswordResetDto dto = new PasswordResetDto("valid_token", "NewPassword123", "DifferentPassword");

        mockMvc.perform(post("/auth/password-reset/reset")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Unable to reset password."));
    }

    @Test
    @WithAnonymousUser
    void testResetPassword_PasswordTooShort() throws Exception {
        PasswordResetDto dto = new PasswordResetDto("valid_token", "short", "short");

        mockMvc.perform(post("/auth/password-reset/reset")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Données invalides"));
    }
}
