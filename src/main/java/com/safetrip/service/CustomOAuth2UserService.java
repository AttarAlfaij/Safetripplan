package com.safetrip.service;

import com.safetrip.model.User;
import com.safetrip.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wraps Spring Security's default OAuth2 user-loading with Google-specific
 * account linking, while leaving GitHub's behavior completely untouched.
 *
 * GitHub:
 *   registrationId != "google" -> we return the exact same OAuth2User that
 *   Spring's default (parent) loader would have returned. No DB lookups, no
 *   attribute changes. This is functionally identical to the previous setup
 *   where no custom user service was registered at all.
 *
 * Google:
 *   We look up (or create) a row in the existing "users" table by email, so
 *   that a person signing in with Google multiple times always maps to the
 *   same account and no duplicates are created. We then inject a stable
 *   "id" attribute (the internal User.id) into the OAuth2User's attributes,
 *   matching the shape GitHub already provides (an "id" attribute), so the
 *   existing per-user data isolation logic in PlanTripController /
 *   TripPlaceController (which reads attribute "id") keeps working
 *   unchanged for both providers.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"google".equals(registrationId)) {
            // Not Google (i.e. GitHub) — hand back exactly what the default
            // loader returned. GitHub flow is unaffected.
            return oAuth2User;
        }

        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account has no email address available.");
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    // OAuth-created accounts don't have a password. The "password"
                    // column is NOT NULL, so we store a random, never-guessable
                    // bcrypt hash. It cannot be produced by any real password, so
                    // the existing email/password login path simply won't match it —
                    // that path is otherwise untouched.
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    return userRepository.save(newUser);
                });

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        // Provider-agnostic identity, mirrors GitHub's "id" attribute so
        // downstream code that isolates data by attribute "id" needs no changes.
        attributes.put("id", user.getId().toString());

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, "email");
    }
}
