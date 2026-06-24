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

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", oAuth2User.getAttribute("login"));
        userInfo.put("avatarUrl", oAuth2User.getAttribute("avatar_url"));
        userInfo.put("name", oAuth2User.getAttribute("name") != null 
                             ? oAuth2User.getAttribute("name") 
                             : oAuth2User.getAttribute("login"));
        
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(userInfo);
    }
}
