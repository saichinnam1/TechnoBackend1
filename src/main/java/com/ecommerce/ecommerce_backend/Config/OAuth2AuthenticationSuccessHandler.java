package com.ecommerce.ecommerce_backend.Config;

import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import com.ecommerce.ecommerce_backend.Service.UserService;
import com.ecommerce.ecommerce_backend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public OAuth2AuthenticationSuccessHandler() {
        super("https://svlnteckart.netlify.app/auth/success");
        setUseReferer(false);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        logger.info("Handling OAuth2 authentication success for request: {}", request.getRequestURI());

        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            logger.error("Authenticated user is not an OidcUser: {}", authentication.getPrincipal());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authenticated user is not an OidcUser");
            return;
        }

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName() != null ? oidcUser.getFullName() : email != null ? email.split("@")[0] : null;

        if (email == null) {
            logger.error("Email not found in OidcUser attributes");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found in OidcUser");
            return;
        }

        final String normalizedEmail = email.toLowerCase();
        logger.debug("Processing OAuth2 user with email: {}", normalizedEmail);

        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            logger.info("User with email {} not found, registering new user", normalizedEmail);
            User newUser = new User();
            newUser.setEmail(normalizedEmail);

            String baseUsername = (name != null ? name.replaceAll("[^a-zA-Z0-9]", "").toLowerCase() : normalizedEmail.split("@")[0]).toLowerCase();
            String username = baseUsername;
            int counter = 1;

            while (userRepository.findByUsername(username).isPresent()) {
                username = baseUsername + counter++;
                logger.debug("Username {} already exists, trying new username: {}", baseUsername, username);
            }

            newUser.setUsername(username);
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            logger.info("Generated username: {} for new user with email: {}", username, normalizedEmail);

            try {
                return userService.registerUser(newUser);
            } catch (Exception e) {
                logger.error("Failed to register new user with email: {}. Error: {}", normalizedEmail, e.getMessage(), e);
                throw new RuntimeException("Failed to register new user", e);
            }
        });

        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), Set.of("USER"));
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        logger.debug("Generated JWT tokens for user: {}", user.getUsername());

        String redirectUrl = getDefaultTargetUrl() + "?token=" + accessToken + "&refreshToken=" + refreshToken;
        logger.info("Redirecting to: {} after successful OAuth2 authentication", redirectUrl);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
