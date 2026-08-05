package RNCP.TrocSkillHub.Repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import RNCP.TrocSkillHub.Models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

    /*
     * Hibernate refuses to fetch-join more than one bag at a time
     * (MultipleBagFetchException), so each List collection of User needs its own
     * query. Callers that want the whole profile chain the findByIdWith* methods
     * inside a single transaction: Hibernate resolves them against the same
     * managed instance, so the collections end up populated on one entity.
     */

    @EntityGraph(attributePaths = { "userKnowledge", "userKnowledge.knowledge" })
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithKnowledge(@Param("id") Long id);

    @EntityGraph(attributePaths = { "userKnowledge", "userKnowledge.knowledge" })
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithKnowledge(@Param("email") String email);

    @EntityGraph(attributePaths = { "userKnowledge", "userKnowledge.knowledge" })
    @Query("SELECT u FROM User u")
    List<User> findAllWithKnowledge();

    @EntityGraph(attributePaths = "education")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithEducation(@Param("id") Long id);

    @EntityGraph(attributePaths = "experience")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithExperience(@Param("id") Long id);

    @EntityGraph(attributePaths = "project")
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithProject(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM User u WHERE u.email = :email")
    Boolean existsByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.city = :city")
    List<User> findByCity(@Param("city") String city);

    @Query("SELECT u FROM User u WHERE u.country = :country")
    List<User> findByCountry(@Param("country") String country);
}
