package RNCP.TrocSkillHub.Repositories;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import RNCP.TrocSkillHub.Models.User;
@Repository
public interface UserRepository extends JpaRepository <User, Serial>{ 
    Optional <User> findByEmail(String email);
    Boolean existsByEmail(String email);
    List <User> findByCity(String city);
    List <User> findByCountry(String country);
}


