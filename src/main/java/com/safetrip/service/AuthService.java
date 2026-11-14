package com.safetrip.service;

import com.safetrip.model.User;
import com.safetrip.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // In-memory session store: token -> userId
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user. Throws if email is already taken.
     */
    public String register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        return createSession(user.getId());
    }

    /**
     * Login an existing user. Throws if email not found or password wrong.
     */
    public String login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found with this email."));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect password.");
        }
        return createSession(user.getId());
    }

    /**
     * Invalidate a session token.
     */
    public void logout(String token) {
        sessions.remove(token);
    }

    /**
     * Get the user associated with a session token.
     */
    public Optional<User> getUserFromToken(String token) {
        if (token == null)
            return Optional.empty();
        Long userId = sessions.get(token);
        if (userId == null)
            return Optional.empty();
        return userRepository.findById(userId);
    }

    private String createSession(Long userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);
        return token;
    }
}
