package com.ecommerce.ecommerce_backend.Service;

import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.entity.Admin;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import com.ecommerce.ecommerce_backend.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String ROLE_PREFIX = "ROLE_";
    private static final Set<String> USER_ROLE = Set.of("USER");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            logger.error("Username is null or empty");
            throw new UsernameNotFoundException("Username cannot be null or empty");
        }

        logger.debug("Loading user by username: {}", username);

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            logger.debug("Found User: {}", user.getUsername());
            return new org.springframework.security.core.userdetails.User(
                    user.getUsername(),
                    user.getPassword(),
                    USER_ROLE.stream()
                            .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                            .collect(Collectors.toList())
            );
        }

        Optional<Admin> adminOpt = adminRepository.findByUsername(username);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            logger.debug("Found Admin: {}, roles: {}", admin.getUsername(), admin.getRoles());
            return new org.springframework.security.core.userdetails.User(
                    admin.getUsername(),
                    admin.getPassword(),
                    admin.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                            .collect(Collectors.toList())
            );
        }

        logger.warn("User not found with username: {}", username);
        throw new UsernameNotFoundException("User not found with username: " + username);
    }

    public User registerUser(User user) {
        validateUser(user);
        logger.debug("Registering user: {}", user.getUsername());
        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {}", savedUser.getUsername());
        return savedUser;
    }

    public Admin registerAdmin(Admin admin) {
        validateAdmin(admin);
        logger.debug("Registering admin: {}", admin.getUsername());
        Admin savedAdmin = adminRepository.save(admin);
        logger.info("Admin registered successfully: {}", savedAdmin.getUsername());
        return savedAdmin;
    }

    public User findByEmail(String email) throws UserNotFoundException {
        if (email == null || email.trim().isEmpty()) {
            logger.error("Email is null or empty");
            throw new UserNotFoundException("Email cannot be null or empty");
        }

        logger.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UserNotFoundException("User not found with email: " + email);
                });
    }

    public Admin findAdminByEmail(String email) throws UserNotFoundException {
        if (email == null || email.trim().isEmpty()) {
            logger.error("Email is null or empty");
            throw new UserNotFoundException("Email cannot be null or empty");
        }

        logger.debug("Finding admin by email: {}", email);
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Admin not found with email: {}", email);
                    return new UserNotFoundException("Admin not found with email: " + email);
                });
    }

    private void validateUser(User user) {
        if (user == null) {
            logger.error("User object is null");
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            logger.error("Username is null or empty for user");
            throw new IllegalArgumentException("Username is required");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            logger.error("Email is null or empty for user");
            throw new IllegalArgumentException("Email is required");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            logger.error("Password is null or empty for user");
            throw new IllegalArgumentException("Password is required");
        }
    }

    private void validateAdmin(Admin admin) {
        if (admin == null) {
            logger.error("Admin object is null");
            throw new IllegalArgumentException("Admin cannot be null");
        }
        if (admin.getUsername() == null || admin.getUsername().trim().isEmpty()) {
            logger.error("Username is null or empty for admin");
            throw new IllegalArgumentException("Username is required");
        }
        if (admin.getEmail() == null || admin.getEmail().trim().isEmpty()) {
            logger.error("Email is null or empty for admin");
            throw new IllegalArgumentException("Email is required");
        }
        if (admin.getPassword() == null || admin.getPassword().trim().isEmpty()) {
            logger.error("Password is null or empty for admin");
            throw new IllegalArgumentException("Password is required");
        }
        if (admin.getRoles() == null || admin.getRoles().isEmpty()) {
            logger.error("Roles are null or empty for admin");
            throw new IllegalArgumentException("Roles are required for admin");
        }
    }
}

class UserNotFoundException extends Exception {
    public UserNotFoundException(String message) {
        super(message);
    }
}