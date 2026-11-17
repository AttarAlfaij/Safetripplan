package com.safetrip.controller;

import com.safetrip.model.User;
import com.safetrip.service.UserService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUser() {
        User user = userService.getAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .cacheControl(CacheControl.noStore().mustRevalidate())
                    .body(Map.of("error", "Not authenticated"));
        }

        String displayName = user.getName() != null && !user.getName().isBlank()
                ? user.getName()
                : user.getEmail();

        String jwtToken = userService.generateTokenForUser(user);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("userId", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("username", user.getEmail());
        userInfo.put("name", displayName);
        userInfo.put("avatarUrl", user.getAvatarUrl());
        userInfo.put("token", jwtToken);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(userInfo);
    }
}
