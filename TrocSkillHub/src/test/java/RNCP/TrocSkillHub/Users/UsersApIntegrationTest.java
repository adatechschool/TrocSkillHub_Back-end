package RNCP.TrocSkillHub.Users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class UsersApiIntegrationTest {

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


    private String jwtToken;

 @BeforeEach
void setUp() throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "nom": "Dupont",
                  "prenom": "Jean",
                  "email": "jean.dupont@test.fr",
                  "password": "Password1!",
                  "city": "Paris",
                  "country": "France"
                }
                """));
    MvcResult loginResult = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email": "jean.dupont@test.fr",
                  "password": "Password1!"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Connexion réussie"))
            .andExpect(jsonPath("$.token").doesNotExist())
            .andReturn();

    String setCookie = loginResult.getResponse().getHeader("Set-Cookie");
    if (setCookie == null || !setCookie.startsWith("jwt=")) {
        throw new IllegalStateException("JWT cookie missing from login response");
    }
    jwtToken = setCookie.substring("jwt=".length(), setCookie.indexOf(';'));

}
@Test
void getAllUsers_withJwt_returnsUsersWithNames() throws Exception {

    mockMvc.perform(get("/users")
            .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.lastName == 'Dupont')].firstName").value("Jean"));
}

@Test
void getAllUsers_withJwt_doesNotExposeSensitiveData() throws Exception {

    mockMvc.perform(get("/users")
            .header("Authorization", "Bearer " + jwtToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").doesNotExist())
            .andExpect(jsonPath("$[0].phoneNumber").doesNotExist())
            .andExpect(jsonPath("$[0].address").doesNotExist())
            .andExpect(jsonPath("$[0].description").doesNotExist());
}
}
