package RNCP.TrocSkillHub.Controllers;

import java.io.Long;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Services.UserService;


@RestController
@RequestMapping("/api/users")
public class UserController {

    
    private final UserService userService;

    @Autowired
    public UserController (UserService userService) {
        this.userService = userService;
    }

    // Récuperer tous les utilisateurs 
    public ResponseEntity<List<User>> getAllUsers(){
        List <User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // Récuperer les données utilisateur à partir d'un id
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById (@PathVariable Long id) {
        return userService.getUserById(id)
        .map(user -> ResponseEntity.ok(user))
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(null));
    }

    // Mettre à jour les données d'un utilisateur
    
}
