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
 * Custom OAuth2 user service for GitHub and Google standard OAuth2 flows.
 * Maps each OAuth account to a persistent User record in the database.
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

        if ("google".equalsIgnoreCase(registrationId)) {
            String email = oAuth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                throw new OAuth2AuthenticationException("Google account has no email address available.");
            }
            final String cleanEmail = email.trim().toLowerCase();
            String name = oAuth2User.getAttribute("name");
            String picture = oAuth2User.getAttribute("picture");

            User user = userRepository.findByEmail(cleanEmail)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(cleanEmail);
                        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                        newUser.setName(name != null ? name : cleanEmail);
                        newUser.setAvatarUrl(picture);
                        newUser.setProvider("GOOGLE");
                        return userRepository.save(newUser);
                    });

            Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
            attributes.put("id", user.getId().toString());
            attributes.put("userId", user.getId());

            return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, "email");
        }

        // GitHub provider
        Object ghIdObj = oAuth2User.getAttribute("id");
        String ghId = ghIdObj != null ? ghIdObj.toString() : null;
        String login = oAuth2User.getAttribute("login");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");

        String userEmail;
        if (email != null && !email.isBlank()) {
            userEmail = email.trim().toLowerCase();
        } else if (ghId != null) {
            userEmail = "gh_" + ghId + "@github.user";
        } else if (login != null) {
            userEmail = login.toLowerCase() + "@github.user";
        } else {
            userEmail = "gh_" + UUID.randomUUID() + "@github.user";
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(userEmail);
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setName(name != null ? name : (login != null ? login : userEmail));
                    newUser.setAvatarUrl(avatarUrl);
                    newUser.setProvider("GITHUB");
                    return userRepository.save(newUser);
                });

        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        // Provide stable user id mapped to database User.id
        attributes.put("id", user.getId().toString());
        attributes.put("userId", user.getId());
        if (!attributes.containsKey("email")) {
            attributes.put("email", user.getEmail());
        }

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        if (nameAttributeKey == null || nameAttributeKey.isBlank()) {
            nameAttributeKey = "id";
        }

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, nameAttributeKey);
    }
}
