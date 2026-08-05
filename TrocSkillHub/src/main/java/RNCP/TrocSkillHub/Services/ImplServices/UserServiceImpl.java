package RNCP.TrocSkillHub.Services.ImplServices;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import RNCP.TrocSkillHub.DTOs.EducationDTO;
import RNCP.TrocSkillHub.DTOs.ExperienceDTO;
import RNCP.TrocSkillHub.DTOs.ProjectDTO;
import RNCP.TrocSkillHub.DTOs.UserKnowledgeDTO;
import RNCP.TrocSkillHub.DTOs.UserRequestDTO;
import RNCP.TrocSkillHub.Mappers.UserMapper;
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

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            KnowledgeRepository knowledgeRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Cet email existe déjà!");
        }

        validatePasswordStrength(user.getPassword());

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    /**
     * The public listing only exposes skills and needs, so the heavier profile
     * collections are deliberately left out of the fetch plan.
     */
    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAllWithKnowledge();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return withProfileCollections(userRepository.findByIdWithKnowledge(id));
    }

    @Override
    @Transactional
    public User updateUser(Long id, UserRequestDTO requestDTO) {
        User existingUser = withProfileCollections(userRepository.findByIdWithKnowledge(id))
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + id));

        ensureCollections(existingUser);

        String newEmail = requestDTO.email();
        if (newEmail == null || newEmail.isBlank()) {
            throw new RuntimeException("L'email est obligatoire");
        }

        if (!Objects.equals(existingUser.getEmail(), newEmail)
                && userRepository.existsByEmail(newEmail)) {
            throw new RuntimeException("Cet email existe déjà!");
        }

        existingUser.setFirstName(requestDTO.firstName());
        existingUser.setLastName(requestDTO.lastName());
        existingUser.setAddress(requestDTO.address());
        existingUser.setEmail(requestDTO.email());
        existingUser.setCity(requestDTO.city());
        existingUser.setCountry(requestDTO.country());
        existingUser.setPhoneNumber(requestDTO.phoneNumber());
        existingUser.setDescription(requestDTO.description());
        existingUser.setUpdatedAt(LocalDate.now());

        updatePasswordIfValid(existingUser, requestDTO.password());

        syncEducation(existingUser, requestDTO.education());
        syncExperience(existingUser, requestDTO.experience());
        syncProjects(existingUser, requestDTO.project());
        syncUserKnowledge(existingUser, requestDTO.skills(), requestDTO.needs());

        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public User patchUser(Long id, UserRequestDTO requestDTO) {
        User existingUser = withProfileCollections(userRepository.findByIdWithKnowledge(id))
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + id));

        ensureCollections(existingUser);

        if (requestDTO.email() != null) {
            if (requestDTO.email().isBlank()) {
                throw new RuntimeException("L'email est obligatoire");
            }
            if (!Objects.equals(existingUser.getEmail(), requestDTO.email())
                    && userRepository.existsByEmail(requestDTO.email())) {
                throw new RuntimeException("Cet email existe déjà!");
            }
            existingUser.setEmail(requestDTO.email());
        }

        if (requestDTO.firstName() != null) {
            existingUser.setFirstName(requestDTO.firstName());
        }
        if (requestDTO.lastName() != null) {
            existingUser.setLastName(requestDTO.lastName());
        }
        if (requestDTO.address() != null) {
            existingUser.setAddress(requestDTO.address());
        }
        if (requestDTO.city() != null) {
            existingUser.setCity(requestDTO.city());
        }
        if (requestDTO.country() != null) {
            existingUser.setCountry(requestDTO.country());
        }
        if (requestDTO.phoneNumber() != null) {
            existingUser.setPhoneNumber(requestDTO.phoneNumber());
        }
        if (requestDTO.description() != null) {
            existingUser.setDescription(requestDTO.description());
        }

        existingUser.setUpdatedAt(LocalDate.now());
        updatePasswordIfValid(existingUser, requestDTO.password());

        if (requestDTO.education() != null) {
            syncEducation(existingUser, requestDTO.education());
        }
        if (requestDTO.experience() != null) {
            syncExperience(existingUser, requestDTO.experience());
        }
        if (requestDTO.project() != null) {
            syncProjects(existingUser, requestDTO.project());
        }
        patchUserKnowledge(existingUser, requestDTO.skills(), requestDTO.needs());

        return userRepository.save(existingUser);
    }

    private void patchUserKnowledge(
            User user,
            List<UserKnowledgeDTO> skills,
            List<UserKnowledgeDTO> needs) {
        if (skills == null && needs == null) {
            return;
        }
        if (skills != null && needs != null) {
            syncUserKnowledge(user, skills, needs);
            return;
        }
        if (skills != null) {
            user.getUserKnowledge().removeIf(uk -> uk.getType() == KnowledgeType.SKILL);
            addUserKnowledge(user, skills, KnowledgeType.SKILL);
        }
        if (needs != null) {
            user.getUserKnowledge().removeIf(uk -> uk.getType() == KnowledgeType.NEED);
            addUserKnowledge(user, needs, KnowledgeType.NEED);
        }
    }

    /**
     * Completes a user already loaded with its knowledge: one extra query per
     * remaining collection, because Hibernate cannot fetch several bags at once.
     * Must run inside the caller's transaction so every query targets the same
     * managed instance.
     */
    private Optional<User> withProfileCollections(Optional<User> user) {
        user.map(User::getId).ifPresent(id -> {
            userRepository.findByIdWithEducation(id);
            userRepository.findByIdWithExperience(id);
            userRepository.findByIdWithProject(id);
        });
        return user;
    }

    private void ensureCollections(User user) {
        if (user.getUserKnowledge() == null) {
            user.setUserKnowledge(new ArrayList<>());
        }
        if (user.getEducation() == null) {
            user.setEducation(new ArrayList<>());
        }
        if (user.getExperience() == null) {
            user.setExperience(new ArrayList<>());
        }
        if (user.getProject() == null) {
            user.setProject(new ArrayList<>());
        }
    }

    private void updatePasswordIfValid(User user, String password) {
        if (password == null || password.isBlank()) {
            return;
        }
        try {
            validatePasswordStrength(password);
            user.setPassword(passwordEncoder.encode(password));
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("mot de passe")) {
                return;
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return withProfileCollections(userRepository.findByEmailWithKnowledge(email));
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public List<User> getUsersByCity(String city) {
        return userRepository.findByCity(city);
    }

    @Override
    public List<User> getUsersByCountry(String country) {
        return userRepository.findByCountry(country);
    }

    private void syncEducation(User user, List<EducationDTO> educationDTOs) {
        user.getEducation().clear();
        if (educationDTOs == null) {
            return;
        }
        for (EducationDTO dto : educationDTOs) {
            Education education = userMapper.toEntity(dto);
            education.setUser(user);
            user.getEducation().add(education);
        }
    }

    private void syncExperience(User user, List<ExperienceDTO> experienceDTOs) {
        user.getExperience().clear();
        if (experienceDTOs == null) {
            return;
        }
        for (ExperienceDTO dto : experienceDTOs) {
            Experience experience = userMapper.toEntity(dto);
            experience.setUser(user);
            user.getExperience().add(experience);
        }
    }

    private void syncProjects(User user, List<ProjectDTO> projectDTOs) {
        user.getProject().clear();
        if (projectDTOs == null) {
            return;
        }
        for (ProjectDTO dto : projectDTOs) {
            Project project = userMapper.toEntity(dto);
            project.setUser(user);
            user.getProject().add(project);
        }
    }

    private void syncUserKnowledge(
            User user,
            List<UserKnowledgeDTO> skills,
            List<UserKnowledgeDTO> needs) {
        user.getUserKnowledge().clear();
        addUserKnowledge(user, skills, KnowledgeType.SKILL);
        addUserKnowledge(user, needs, KnowledgeType.NEED);
    }

    private void addUserKnowledge(User user, List<UserKnowledgeDTO> items, KnowledgeType type) {
        if (items == null) {
            return;
        }
        for (UserKnowledgeDTO dto : items) {
            if (dto.knowledgeId() == null) {
                throw new RuntimeException("knowledgeId est obligatoire pour les compétences et besoins");
            }
            Knowledge knowledge = knowledgeRepository.findById(dto.knowledgeId())
                    .orElseThrow(() -> new RuntimeException(
                            "Connaissance introuvable avec l'id: " + dto.knowledgeId()));
            user.getUserKnowledge().add(new UserKnowledge(user, knowledge, type, dto.level()));
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 8 caractères");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new RuntimeException("Le mot de passe doit contenir au moins une majuscule");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new RuntimeException("Le mot de passe doit contenir au moins une minuscule");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new RuntimeException("Le mot de passe doit contenir au moins un chiffre");
        }
        if (!password.matches(".*[@#$%^&+=!?*].*")) {
            throw new RuntimeException(
                    "Le mot de passe doit contenir au moins un caractère spécial (@#$%^&+=!?*)");
        }
    }
}
