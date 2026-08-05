package RNCP.TrocSkillHub.Users;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import RNCP.TrocSkillHub.Models.Education;
import RNCP.TrocSkillHub.Models.Experience;
import RNCP.TrocSkillHub.Models.Knowledge;
import RNCP.TrocSkillHub.Models.KnowledgeType;
import RNCP.TrocSkillHub.Models.Project;
import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Models.UserKnowledge;
import RNCP.TrocSkillHub.Repositories.KnowledgeRepository;
import RNCP.TrocSkillHub.Repositories.UserRepository;
import RNCP.TrocSkillHub.Services.UserService;

import jakarta.persistence.EntityManager;

/**
 * Guards the fetch plans that replaced the manual Hibernate.initialize calls:
 * the collections must still be fully loaded, and the public listing must not
 * degrade into one query per user.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class UserProfileFetchIntegrationTest {

    private static final int SEEDED_USERS = 3;
    private static final String EMAIL_PREFIX = "profile.fetch.";

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("trocskillhubdb_test");

    @DynamicPropertySource
    static void registerDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KnowledgeRepository knowledgeRepository;

    @Autowired
    private EntityManager entityManager;

    private Long firstUserId;
    private String firstUserEmail;

    @BeforeEach
    void seedFullProfiles() {
        List<Knowledge> knowledges = knowledgeRepository.findAll();
        assertThat(knowledges).hasSizeGreaterThanOrEqualTo(2);

        for (int i = 0; i < SEEDED_USERS; i++) {
            User user = new User();
            user.setFirstName("Ada" + i);
            user.setLastName("Lovelace" + i);
            user.setEmail(EMAIL_PREFIX + i + "@test.fr");
            user.setPassword("hashed-placeholder");

            user.getEducation().add(
                    new Education("Master CDA", "CNAM", LocalDate.of(2023, 9, 1), LocalDate.of(2025, 6, 30), user));
            user.getExperience().add(
                    new Experience("OVHcloud", "Développeuse", LocalDate.of(2024, 1, 1), null, user));
            user.getProject().add(
                    new Project("TrocSkillHub", "Plateforme d'échange", "https://example.test",
                            LocalDate.of(2025, 1, 1), null, user));
            user.getUserKnowledge().add(
                    new UserKnowledge(user, knowledges.get(0), KnowledgeType.SKILL, "Avancé"));
            user.getUserKnowledge().add(
                    new UserKnowledge(user, knowledges.get(1), KnowledgeType.NEED, "Débutant"));

            User saved = userRepository.save(user);
            if (i == 0) {
                firstUserId = saved.getId();
                firstUserEmail = saved.getEmail();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void getUserById_loadsEveryProfileCollection() {
        User loaded = userService.getUserById(firstUserId).orElseThrow();

        assertThat(loaded.getEducation()).hasSize(1);
        assertThat(loaded.getExperience()).hasSize(1);
        assertThat(loaded.getProject()).hasSize(1);
        assertThat(loaded.getUserKnowledge()).hasSize(2);
    }

    @Test
    void getUserByEmail_loadsEveryProfileCollection() {
        User loaded = userService.getUserByEmail(firstUserEmail).orElseThrow();

        assertThat(loaded.getEducation()).hasSize(1);
        assertThat(loaded.getExperience()).hasSize(1);
        assertThat(loaded.getProject()).hasSize(1);
        assertThat(loaded.getUserKnowledge()).hasSize(2);
    }

    @Test
    void getUserById_usesOneQueryPerCollectionAndNotOnePerRow() {
        Statistics statistics = resetStatistics();

        User loaded = userService.getUserById(firstUserId).orElseThrow();
        touchProfile(loaded);

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(4);
    }

    @Test
    void getAllUsers_readsSkillsAndNeedsInASingleQuery() {
        Statistics statistics = resetStatistics();

        List<User> users = userService.getAllUsers();
        List<User> seeded = users.stream()
                .filter(user -> user.getEmail().startsWith(EMAIL_PREFIX))
                .toList();
        seeded.forEach(user -> assertThat(user.getUserKnowledge()).hasSize(2));

        assertThat(seeded).hasSize(SEEDED_USERS);
        assertThat(users).extracting(User::getEmail).doesNotHaveDuplicates();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    private Statistics resetStatistics() {
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();
        return statistics;
    }

    private void touchProfile(User user) {
        assertThat(user.getUserKnowledge()).isNotEmpty();
        assertThat(user.getEducation()).isNotEmpty();
        assertThat(user.getExperience()).isNotEmpty();
        assertThat(user.getProject()).isNotEmpty();
    }
}
