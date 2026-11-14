package com.safetrip.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/user")
    public ResponseEntity<?> getUser(@AuthenticationPrincipal OAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return ResponseEntity.status(401)
                    .cacheControl(CacheControl.noStore().mustRevalidate())
                    .body(Map.of("error", "Not authenticated"));
        }

        // GitHub attributes ("login", "avatar_url") — unchanged from before.
        String username = oAuth2User.getAttribute("login");
        String avatarUrl = oAuth2User.getAttribute("avatar_url");
        String name = oAuth2User.getAttribute("name");

        // Google doesn't have a "login" attribute; fall back to its own
        // attribute names ("email", "picture") only when GitHub's aren't present.
        if (username == null) {
            username = oAuth2User.getAttribute("email");
            avatarUrl = oAuth2User.getAttribute("picture");
        }
        if (name == null) {
            name = username;
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", username);
        userInfo.put("avatarUrl", avatarUrl);
        userInfo.put("name", name);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(userInfo);
    }
}
