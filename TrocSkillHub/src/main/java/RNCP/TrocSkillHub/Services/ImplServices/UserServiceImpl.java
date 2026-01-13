package RNCP.TrocSkillHub.Services.ImplServices;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Repositories.UserRepository;
import RNCP.TrocSkillHub.Services.UserService;

public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository ){
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(User user){
       //vérif email
       if(userRepository.existsByEmail(user.getEmail())){
        throw new RuntimeException("cet email existe dejà!");
       }
       return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers(){

        return userRepository.findAll();
    }

    @Override
    public Optional<User> getUserById(Serial id){
        return userRepository.findById(id);
    }

    @Override
    public User updateUser(Serial id, User user){
        return userRepository.findById(id)
        .map(existingUser -> {
            existingUser.setFirstName(user.getFirstName());
            existingUser.setLastName(user.getLastName());
            existingUser.setAddress(user.getAddress());
            existingUser.setEmail(user.getEmail());
            existingUser.setPicture(user.getPicture());
            existingUser.setCity(user.getCity());
            existingUser.setCountry(user.getCountry());
            existingUser.setPhoneNumber(user.getPhoneNumber());
            existingUser.setDescription(user.getDescription());
            return userRepository.save(existingUser);
        })
        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + id));

    }
    @Override
    public void deleteUser(Serial id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé avec l'id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> getUserByEmail(String email){
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email){
       return userRepository.existsByEmail(email);
    }
    
    @Override
    public List<User> getUsersByCity(String city){
        return userRepository.findByCity(city);
    }

    @Override
    public List<User> getUsersByCountry(String country){
        return userRepository.findByCountry(country);
    }

}
