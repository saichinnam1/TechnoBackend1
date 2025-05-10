package com.ecommerce.ecommerce_backend.Config;

import com.ecommerce.ecommerce_backend.Service.UserService;
import com.ecommerce.ecommerce_backend.Config.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtFilter jwtFilter;
    private final UserService userService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    @Value("${allowed.origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtFilter jwtFilter, UserService userService, OAuth2AuthenticationSuccessHandler successHandler,
                          AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository) {
        this.jwtFilter = jwtFilter;
        this.userService = userService;
        this.successHandler = successHandler;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring Spring Security filter chain");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> {
                    logger.debug("Disabling CSRF protection");
                    csrf.disable();
                })
                .sessionManagement(session -> {
                    logger.debug("Setting session management to STATELESS");
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            logger.error("Unauthorized access to {}: {}", request.getRequestURI(), authException.getMessage());
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\": false, \"message\": \"Unauthorized\", \"error\": \"" + authException.getMessage() + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            logger.error("Access denied to {}: {}", request.getRequestURI(), accessDeniedException.getMessage());
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\": false, \"message\": \"Access denied\", \"error\": \"" + accessDeniedException.getMessage() + "\"}");
                        })
                )
                .authorizeHttpRequests(authorize -> {
                    logger.debug("Configuring request matchers");
                    authorize
                            .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/**", "/api/reset-password/**", "/oauth2/**", "/login/oauth2/code/**", "/api/oauth2/authorization/**", "/favicon.ico", "/accounts/**", "/error", "/api/products", "/api/products/**", "/api/contact").permitAll()
                            .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> {
                    logger.debug("Configuring OAuth2 login");
                    oauth2
                            .authorizationEndpoint(authorization -> authorization
                                    .authorizationRequestRepository(authorizationRequestRepository)
                            )
                            .successHandler(successHandler)
                            .failureHandler((request, response, exception) -> {
                                logger.error("OAuth2 login failed for request {}: {}", request.getRequestURI(), exception.getMessage());
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"success\": false, \"message\": \"OAuth2 login failed\", \"error\": \"" + exception.getMessage() + "\"}");
                            });
                });

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        logger.info("Spring Security filter chain configured successfully");
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        logger.debug("Configuring CORS");
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}