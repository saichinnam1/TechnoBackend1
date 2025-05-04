package com.ecommerce.ecommerce_backend.Config;

import com.ecommerce.ecommerce_backend.Service.UserService;
import com.ecommerce.ecommerce_backend.Config.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtFilter jwtFilter;
    private final UserService userService;
    private final OAuth2AuthenticationSuccessHandler successHandler;
    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

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
    public CorsFilter corsFilter() {
        logger.debug("Configuring CORS Filter");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://accounts.google.com",
                "https://ecommerce-frontend-hf4x.vercel.app",
                "https://ecommerce-frontend-hf4x-git-master-saichinnam1s-projects.vercel.app",
                "https://ecommerce-frontend-hf4x-8uhzzjj11-saichinnam1s-projects.vercel.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
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
                            // Permit OPTIONS requests for all endpoints to handle CORS pre-flight
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            // Public endpoints
                            .requestMatchers("/api/auth/login").permitAll()
                            .requestMatchers("/api/auth/register").permitAll()
                            .requestMatchers("/api/auth/refresh").permitAll()
                            .requestMatchers("/api/auth/oauth2/success").permitAll()
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/api/reset-password/**").permitAll()
                            .requestMatchers("/api/products").permitAll()
                            .requestMatchers("/api/products/**").permitAll()
                            .requestMatchers("/api/contact").permitAll()
                            .requestMatchers("/uploads/**").permitAll()
                            .requestMatchers("/favicon.ico").permitAll()
                            .requestMatchers("/error").permitAll()
                            // OAuth2 endpoints
                            .requestMatchers("/oauth2/**").permitAll()
                            .requestMatchers("/login/oauth2/code/**").permitAll()
                            .requestMatchers("/api/oauth2/authorization/**").permitAll()
                            .requestMatchers("/accounts/**").permitAll()
                            // Admin endpoints
                            .requestMatchers("/api/auth/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            // All other requests require authentication
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
        http.addFilterBefore(corsFilter(), JwtFilter.class); // Ensure CORS filter runs before security filters

        logger.info("Spring Security filter chain configured successfully");
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        logger.debug("Configuring CORS");
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://accounts.google.com",
                "https://ecommerce-frontend-hf4x.vercel.app",
                "https://ecommerce-frontend-hf4x-git-master-saichinnam1s-projects.vercel.app",
                "https://ecommerce-frontend-hf4x-8uhzzjj11-saichinnam1s-projects.vercel.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}