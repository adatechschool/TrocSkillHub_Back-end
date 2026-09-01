package RNCP.TrocSkillHub.Controllers;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import RNCP.TrocSkillHub.Config.AuditLogger;
import RNCP.TrocSkillHub.Config.JwtService;
import RNCP.TrocSkillHub.DTOs.LoginRequest;
import RNCP.TrocSkillHub.DTOs.RegisterRequest;
import RNCP.TrocSkillHub.Models.User;
import RNCP.TrocSkillHub.Repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final AuditLogger auditLogger;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.cookie.secure:true}") boolean cookieSecure,
            @Value("${app.cookie.same-site:Lax}") String cookieSameSite,
            AuditLogger auditLogger) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
        this.auditLogger = auditLogger;
    }

    /**
     * Issues the XSRF-TOKEN cookie and returns the raw token as JSON.
     * Needed for cross-origin SPAs that cannot read the API cookie via document.cookie.
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName(),
                "parameterName", csrfToken.getParameterName());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest body) {
        String nom = body.nom();
        String prenom = body.prenom();
        String email = body.email();
        String password = body.password();
        String city = body.city();
        String country = body.country();

        try {
            if (userRepository.existsByEmail(email)) {
                auditLogger.warning("AUTH_REGISTER_EMAIL_EXISTS");
                return ResponseEntity.status(400)
                        .body(Map.of("error", "Cet email existe déjà"));
            }

            User newUser = new User();
            newUser.setFirstName(prenom);
            newUser.setLastName(nom);
            newUser.setEmail(email);
            newUser.setPassword(passwordEncoder.encode(password));
            newUser.setCity(city);
            newUser.setCountry(country);

            userRepository.save(newUser);
            auditLogger.info("AUTH_REGISTER_SUCCESS");

            String token = jwtService.generateToken(email);
            ResponseCookie cookie = buildJwtCookie(token, Duration.ofDays(1));

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("message", "Inscription réussie"));

        } catch (Exception e) {
            auditLogger.severe("AUTH_REGISTER_ERROR");
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur lors de l'inscription"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body) {
        String email = body.email();
        String password = body.password();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));

            auditLogger.info("AUTH_LOGIN_SUCCESS");

            String token = jwtService.generateToken(email);
            ResponseCookie cookie = buildJwtCookie(token, Duration.ofDays(1));

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("message", "Connexion réussie"));

        } catch (AuthenticationException e) {
            auditLogger.warning("AUTH_LOGIN_FAILURE");
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Email ou mot de passe incorrect"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie cookie = buildJwtCookie("", Duration.ZERO);
        auditLogger.info("AUTH_LOGOUT");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("message", "Déconnexion réussie"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            String token = null;

            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }

            if (token == null || token.isBlank()) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Non authentifié"));
            }

            String email = jwtService.extractEmail(token);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            return ResponseEntity.ok(Map.of(
                    "firstName", user.getFirstName(),
                    "lastName", user.getLastName(),
                    "email", user.getEmail(),
                    "city", user.getCity() != null ? user.getCity() : "",
                    "country", user.getCountry() != null ? user.getCountry() : ""));

        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token invalide"));
        }
    }

    private ResponseCookie buildJwtCookie(String token, Duration maxAge) {
        return ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(maxAge)
                .sameSite(cookieSameSite)
                .build();
    }
}
