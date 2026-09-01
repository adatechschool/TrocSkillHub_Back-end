package RNCP.TrocSkillHub.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import RNCP.TrocSkillHub.Services.ImplServices.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFIlter jwtAuthFilter;
    private final AuditAccessDeniedHandler auditAccessDeniedHandler;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            PasswordEncoder passwordEncoder,
            JwtAuthFIlter jwtAuthFilter,
            AuditAccessDeniedHandler auditAccessDeniedHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.auditAccessDeniedHandler = auditAccessDeniedHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(auditAccessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/password-reset/request").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/password-reset/verify").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/password-reset/reset").permitAll()
                .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                .requestMatchers(HttpMethod.POST, "/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/knowledges").permitAll()
                .requestMatchers(HttpMethod.GET, "/knowledges/{id}").permitAll()
                .requestMatchers(HttpMethod.POST, "/knowledges").authenticated()
                .requestMatchers(HttpMethod.PUT, "/knowledges/{id}").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/knowledges/{id}").authenticated()
                .requestMatchers(HttpMethod.POST, "/users").authenticated()
                .requestMatchers(HttpMethod.GET, "/users").authenticated()
                .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/users/me").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/users/me").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/users/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/users/me/profile-document").authenticated()
                .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}