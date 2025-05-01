package com.ecommerce.ecommerce_backend.Controller;

import com.ecommerce.ecommerce_backend.entity.RegistrationRequest;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.entity.Admin;
import com.ecommerce.ecommerce_backend.entity.CustomerOrder;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import com.ecommerce.ecommerce_backend.repository.AdminRepository;
import com.ecommerce.ecommerce_backend.repository.CustomerOrderRepository;
import com.ecommerce.ecommerce_backend.Service.UserService;
import com.ecommerce.ecommerce_backend.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          UserRepository userRepository, AdminRepository adminRepository,
                          CustomerOrderRepository customerOrderRepository,
                          UserService userService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.adminRepository = adminRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials, HttpServletResponse response) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (!isValidInput(username) || !isValidInput(password)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        logger.info("Login attempt for username: {}", username);
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            logger.info("Authentication successful for username: {}", username);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Optional<User> userOpt = userRepository.findByUsername(username);
            Optional<Admin> adminOpt = adminRepository.findByUsername(username);
            String accessToken;
            String refreshToken;
            Map<String, Object> responseBody;

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                accessToken = jwtUtil.generateAccessToken(user.getUsername(), Set.of("USER"));
                refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
                responseBody = buildUserResponse(accessToken, refreshToken, user, Set.of("USER"));
            } else if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                accessToken = jwtUtil.generateAccessToken(admin.getUsername(), admin.getRoles());
                refreshToken = jwtUtil.generateRefreshToken(admin.getUsername());
                responseBody = buildUserResponse(accessToken, refreshToken, admin, admin.getRoles());
            } else {
                throw new RuntimeException("User not found");
            }

            response.setHeader("Authorization", TOKEN_PREFIX + accessToken);
            return ResponseEntity.ok(responseBody);
        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed for username: {} - Invalid credentials", username);
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        } catch (Exception e) {
            logger.error("Error during login for username: {} - {}", username, e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegistrationRequest request, HttpServletResponse response) {
        String username = request.getUsername();
        String password = request.getPassword();
        String email = request.getEmail();
        String role = request.getRole();

        if (!isValidInput(username)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (!isValidInput(password)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (!isValidInput(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Valid email is required");
        }

        email = email.toLowerCase();
        username = username.toLowerCase();

        if (userRepository.findByUsername(username).isPresent() || adminRepository.findByUsername(username).isPresent()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.findByEmail(email).isPresent() || adminRepository.findByEmail(email).isPresent()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        if ("ADMIN".equalsIgnoreCase(role)) {
            logger.warn("Unauthorized attempt to register as ADMIN by username: {}", username);
            return buildErrorResponse(HttpStatus.FORBIDDEN, "Admin registration is restricted");
        }

        User user = createUser(username, password, email);
        try {
            User savedUser = userService.registerUser(user);
            String accessToken = jwtUtil.generateAccessToken(savedUser.getUsername(), Set.of("USER"));
            String refreshToken = jwtUtil.generateRefreshToken(savedUser.getUsername());

            response.setHeader("Authorization", TOKEN_PREFIX + accessToken);
            return ResponseEntity.ok(buildUserResponse(accessToken, refreshToken, savedUser, Set.of("USER")));
        } catch (DataIntegrityViolationException e) {
            logger.error("Database error during registration: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.CONFLICT, "Email or username already exists");
        } catch (Exception e) {
            logger.error("Error during registration: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to register user");
        }
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> registerAdmin(@RequestBody RegistrationRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String email = request.getEmail();

        if (!isValidInput(username)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (!isValidInput(password)) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (!isValidInput(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Valid email is required");
        }

        email = email.toLowerCase();
        username = username.toLowerCase();

        if (userRepository.findByUsername(username).isPresent() || adminRepository.findByUsername(username).isPresent()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Username already exists");
        }
        if (userRepository.findByEmail(email).isPresent() || adminRepository.findByEmail(email).isPresent()) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Email already exists");
        }

        Set<String> roles = new HashSet<>();
        roles.add("ADMIN");

        Admin admin = createAdmin(username, password, email, roles);
        try {
            Admin savedAdmin = userService.registerAdmin(admin);
            String accessToken = jwtUtil.generateAccessToken(savedAdmin.getUsername(), savedAdmin.getRoles());
            String refreshToken = jwtUtil.generateRefreshToken(savedAdmin.getUsername());
            return ResponseEntity.ok(buildUserResponse(accessToken, refreshToken, savedAdmin, savedAdmin.getRoles()));
        } catch (DataIntegrityViolationException e) {
            logger.error("Database error during admin registration: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.CONFLICT, "Email or username already exists");
        } catch (Exception e) {
            logger.error("Error during admin registration: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to register admin");
        }
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userDetails = new ArrayList<>();

        for (User user : users) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getId());
            userData.put("name", user.getName() != null ? user.getName() : user.getUsername());
            List<CustomerOrder> orders = customerOrderRepository.findByUserId(user.getId());
            userData.put("order", orders.stream().map(order -> order.getId() + ": " + order.getStatus()).toList());
            userData.put("shippingAddress", orders.stream()
                    .map(CustomerOrder::getShippingAddress)
                    .filter(Objects::nonNull)
                    .map(sa -> sa.getFullName() + ", " + sa.getStreetAddress() + ", " + sa.getCity() + ", " + sa.getPostalCode())
                    .findFirst()
                    .orElse("N/A"));
            userData.put("email", user.getEmail());
            userDetails.add(userData);
        }

        return ResponseEntity.ok(userDetails);
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
            String token = authorizationHeader.substring(TOKEN_PREFIX.length());
            String username = jwtUtil.extractUsername(token);
            if (jwtUtil.validateToken(token, username)) {
                try {
                    Optional<User> userOpt = userRepository.findByUsername(username);
                    Optional<Admin> adminOpt = adminRepository.findByUsername(username);
                    if (userOpt.isPresent()) {
                        return ResponseEntity.ok(buildUserResponse(null, null, userOpt.get(), Set.of("USER")));
                    } else if (adminOpt.isPresent()) {
                        return ResponseEntity.ok(buildUserResponse(null, null, adminOpt.get(), adminOpt.get().getRoles()));
                    } else {
                        throw new RuntimeException("User not found");
                    }
                } catch (RuntimeException e) {
                    return buildErrorResponse(HttpStatus.NOT_FOUND, "User not found");
                }
            }
        }
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }

    @GetMapping("/failure")
    public ResponseEntity<Map<String, Object>> handleAuthenticationFailure() {
        logger.error("Authentication failure handled");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        String username = jwtUtil.extractUsername(refreshToken);
        if (username != null && !jwtUtil.isTokenExpired(refreshToken)) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            Optional<Admin> adminOpt = adminRepository.findByUsername(username);
            String newAccessToken;
            String newRefreshToken;
            if (userOpt.isPresent()) {
                newAccessToken = jwtUtil.generateAccessToken(userOpt.get().getUsername(), Set.of("USER"));
                newRefreshToken = jwtUtil.generateRefreshToken(userOpt.get().getUsername());
            } else if (adminOpt.isPresent()) {
                newAccessToken = jwtUtil.generateAccessToken(adminOpt.get().getUsername(), adminOpt.get().getRoles());
                newRefreshToken = jwtUtil.generateRefreshToken(adminOpt.get().getUsername());
            } else {
                throw new RuntimeException("User not found");
            }
            Map<String, Object> response = new HashMap<>();
            response.put("token", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            return ResponseEntity.ok(response);
        }
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUser(@RequestHeader("Authorization") String authorizationHeader) {
        logger.info("Handling request to get user details");

        if (authorizationHeader == null || !authorizationHeader.startsWith(TOKEN_PREFIX)) {
            logger.error("Invalid or missing Authorization header");
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid or missing Authorization header");
        }

        String token = authorizationHeader.substring(TOKEN_PREFIX.length());
        String username = jwtUtil.extractUsername(token);

        if (username == null || !jwtUtil.validateToken(token, username)) {
            logger.error("Invalid or expired token");
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        Map<String, Object> userDetails = new HashMap<>();
        Set<String> roles;

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userDetails.put("id", user.getId());
            userDetails.put("username", user.getUsername());
            userDetails.put("name", user.getName() != null ? user.getName() : user.getUsername());
            userDetails.put("email", user.getEmail());
            roles = Set.of("USER");
        } else if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            userDetails.put("id", admin.getId());
            userDetails.put("username", admin.getUsername());
            userDetails.put("email", admin.getEmail());
            roles = admin.getRoles();
        } else {
            throw new RuntimeException("User not found");
        }

        userDetails.put("roles", roles);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("user", userDetails);
        response.put("isNewUser", false);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/oauth2/success")
    @Transactional
    public ResponseEntity<Map<String, Object>> handleOAuth2Success(HttpServletResponse response) {
        logger.info("Handling OAuth2 success callback");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OAuth2User)) {
            logger.error("No OAuth2 user found in authentication context");
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "OAuth2 authentication failed");
        }

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            logger.error("Invalid or missing email in OAuth2 user attributes: {}", email);
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "Valid email not provided by OAuth2 provider");
        }

        Optional<User> existingUser = userRepository.findByEmail(email.toLowerCase());
        User user;
        boolean isNewUser = false;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            logger.info("Existing user found with email: {}", email);
        } else {
            String username = generateUniqueUsername(name != null ? name : email.split("@")[0]);
            user = new User();
            user.setUsername(username);
            user.setEmail(email.toLowerCase());
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user = userService.registerUser(user);
            isNewUser = true;
            logger.info("Registered new user with email: {} and username: {}", email, username);
        }

        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), Set.of("USER"));
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        response.setHeader("Authorization", TOKEN_PREFIX + accessToken);
        Map<String, Object> responseBody = buildUserResponse(accessToken, refreshToken, user, Set.of("USER"));
        responseBody.put("isNewUser", isNewUser);
        return ResponseEntity.ok(responseBody);
    }

    private String generateUniqueUsername(String baseUsername) {
        baseUsername = baseUsername.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        String username = baseUsername;
        int counter = 1;

        while (userRepository.findByUsername(username).isPresent() || adminRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter++;
            logger.debug("Username {} already exists, trying new username: {}", baseUsername, username);
        }
        return username;
    }

    private boolean isValidInput(String input) {
        return input != null && !input.trim().isEmpty();
    }

    private User createUser(String username, String password, String email) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        return user;
    }

    private Admin createAdmin(String username, String password, String email, Set<String> roles) {
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEmail(email);
        admin.setRoles(roles);
        return admin;
    }

    private Map<String, Object> buildUserResponse(String accessToken, String refreshToken, Object entity, Set<String> roles) {
        Map<String, Object> responseBody = new HashMap<>();
        if (accessToken != null) {
            responseBody.put("token", accessToken);
        }
        if (refreshToken != null) {
            responseBody.put("refreshToken", refreshToken);
        }
        String username = entity instanceof User ? ((User) entity).getUsername() : ((Admin) entity).getUsername();
        String email = entity instanceof User ? ((User) entity).getEmail() : ((Admin) entity).getEmail();
        Long id = entity instanceof User ? ((User) entity).getId() : ((Admin) entity).getId();
        responseBody.put("user", Map.of(
                "id", id,
                "username", username,
                "email", email,
                "roles", roles
        ));
        return responseBody;
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", false);
        responseBody.put("message", message);
        return ResponseEntity.status(status).body(responseBody);
    }
}