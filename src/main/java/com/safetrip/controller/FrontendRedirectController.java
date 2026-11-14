package com.safetrip.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendRedirectController {

    private final String frontendBaseUrl;

    public FrontendRedirectController(
            @Value("${app.frontend-url:http://localhost:5173}") String frontendBaseUrl) {
        this.frontendBaseUrl = stripTrailingSlash(frontendBaseUrl);
    }

    @GetMapping({
            "/",
            "/home",
            "/about",
            "/login",
            "/plan-trip",
            "/dashboard",
            "/my-trip",
            "/risk-map"
    })
    public String redirectToFrontend(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("/".equals(path) || "/home".equals(path)) {
            path = "/";
        }

        StringBuilder target = new StringBuilder(frontendBaseUrl).append(path);
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            target.append('?').append(query);
        }
        return "redirect:" + target;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:5173";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
