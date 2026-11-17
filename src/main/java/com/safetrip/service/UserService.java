package com.safetrip.service;

import com.safetrip.model.User;
import com.safetrip.repository.UserRepository;
import com.safetrip.security.JwtUtil;
import com.safetrip.security.UserPrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class.getName());

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Resolve the currently authenticated user from SecurityContext.
     * Supports JWT-authenticated users, Google OAuth2/OIDC users, GitHub OAuth2 users,
     * and Mock users in tests.
     *
     * Never trusts any user ID passed from client.
     */
    @Transactional
    public User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return getAuthenticatedUser(auth);
    }

    @Transactional
    public User getAuthenticatedUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = auth.getPrincipal();

        // 1. JWT UserPrincipal
        if (principal instanceof UserPrincipal userPrincipal) {
            return userRepository.findById(userPrincipal.getId())
                    .orElse(userPrincipal.getUser());
        }

        // 2. Google OIDC User
        if (principal instanceof OidcUser oidcUser) {
            return resolveGoogleUser(oidcUser);
        }

        // 3. GitHub or generic OAuth2User
        if (principal instanceof OAuth2User oAuth2User) {
            String registrationId = "oauth2";
            if (auth instanceof OAuth2AuthenticationToken oauthToken) {
                registrationId = oauthToken.getAuthorizedClientRegistrationId();
            }
            if ("google".equalsIgnoreCase(registrationId)) {
                return resolveGoogleUser(oAuth2User);
            }
            return resolveGitHubUser(oAuth2User);
        }

        // 4. Spring Security UserDetails (mock users in tests, form login)
        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            return userRepository.findByEmail(username)
                    .orElseGet(() -> {
                        User user = new User();
                        user.setEmail(username);
                        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                        user.setName(username);
                        user.setProvider("LOCAL");
                        return userRepository.save(user);
                    });
        }

        // 5. String username/principal
        if (principal instanceof String username && !username.equalsIgnoreCase("anonymousUser")) {
            return userRepository.findByEmail(username)
                    .orElseGet(() -> {
                        User user = new User();
                        user.setEmail(username);
                        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                        user.setName(username);
                        user.setProvider("LOCAL");
                        return userRepository.save(user);
                    });
        }

        return null;
    }

    private User resolveGoogleUser(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        if (email == null || email.isBlank()) {
            email = oAuth2User.getName();
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account has no email address.");
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

        // Update profile details if changed
        boolean updated = false;
        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            updated = true;
        }
        if (picture != null && !picture.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(picture);
            updated = true;
        }
        if (updated) {
            user = userRepository.save(user);
        }

        return user;
    }

    private User resolveGitHubUser(OAuth2User oAuth2User) {
        Object ghIdObj = oAuth2User.getAttribute("id");
        String ghId = ghIdObj != null ? ghIdObj.toString() : null;
        String login = oAuth2User.getAttribute("login");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");

        // Determine unique email identifier for GitHub user
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

        boolean updated = false;
        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            updated = true;
        }
        if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarUrl);
            updated = true;
        }
        if (updated) {
            user = userRepository.save(user);
        }

        return user;
    }

    public String generateTokenForUser(User user) {
        return jwtUtil.generateToken(user);
    }
}
