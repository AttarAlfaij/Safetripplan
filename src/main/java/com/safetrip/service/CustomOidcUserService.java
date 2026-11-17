package com.safetrip.service;

import com.safetrip.model.User;
import com.safetrip.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            email = oidcUser.getAttribute("email");
        }
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account has no email address available.");
        }

        final String cleanEmail = email.trim().toLowerCase();
        String name = oidcUser.getAttribute("name");
        String picture = oidcUser.getAttribute("picture");

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

        Map<String, Object> attributes = new HashMap<>(oidcUser.getAttributes());
        attributes.put("id", user.getId().toString());
        attributes.put("userId", user.getId());
        attributes.put("dbUserId", user.getId());

        return new DefaultOidcUser(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo(), "email");
    }
}
