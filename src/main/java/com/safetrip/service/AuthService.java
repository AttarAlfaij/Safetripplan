package com.safetrip.service;

import com.safetrip.model.User;
import com.safetrip.repository.UserRepository;
import com.safetrip.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user with email and password.
     * Securely hashes password using BCrypt and generates JWT token.
     */
    @Transactional
    public AuthResult register(String email, String rawPassword, String name) {
        validateEmail(email);
        validatePassword(rawPassword);

        String cleanEmail = email.trim().toLowerCase();

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = new User();
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setName(name != null && !name.isBlank() ? name.trim() : cleanEmail.split("@")[0]);
        user.setProvider("LOCAL");

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateToken(savedUser);

        return new AuthResult(token, savedUser);
    }

    /**
     * Legacy register overload for backward compatibility
     */
    @Transactional
    public String register(String email, String rawPassword) {
        return register(email, rawPassword, null).token();
    }

    /**
     * Authenticate an existing user and generate JWT token.
     */
    @Transactional(readOnly = true)
    public AuthResult login(String email, String rawPassword) {
        if (email == null || email.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        String cleanEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String token = jwtUtil.generateToken(user);
        return new AuthResult(token, user);
    }

    /**
     * Invalidate/logout token.
     */
    public void logout(String token) {
        // Stateless JWTs are primarily invalidated client-side, but this hook allows future blacklisting.
    }

    /**
     * Retrieve user from JWT token.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserFromToken(String token) {
        if (token == null || !jwtUtil.validateToken(token)) {
            return Optional.empty();
        }
        Long userId = jwtUtil.extractUserId(token);
        if (userId != null) {
            Optional<User> user = userRepository.findById(userId);
            if (user.isPresent()) {
                return user;
            }
        }
        String email = jwtUtil.extractEmail(token);
        if (email != null) {
            return userRepository.findByEmail(email);
        }
        return Optional.empty();
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Please provide a valid email address.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
    }

    public record AuthResult(String token, User user) {}
}
