package com.safetrip.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles page-level navigation for the Safe Trip Planner.
 *
 * Public routes  : /  (home)
 * Protected routes: /plan-trip  ,  /dashboard
 *
 * Spring Security (SecurityConfig) enforces authentication on /plan-trip and
 * /dashboard — unauthenticated requests are redirected to GitHub OAuth2 login.
 */
@Controller
public class PageController {

    /**
     * GET /
     * Public home page — accessible by anyone.
     */
    @GetMapping("/")
    public String home() {
        // Serves src/main/resources/static/index.html (or a Thymeleaf template)
        return "forward:/index.html";
    }

    /**
     * GET /home
     * Alias for the home page — also public.
     */
    @GetMapping("/home")
    public String homePage() {
        return "forward:/index.html";
    }

    /**
     * GET /plan-trip
     * Protected — only authenticated users can reach this page.
     * Spring Security redirects unauthenticated requests to /oauth2/authorization/github.
     *
     * @param oAuth2User the currently authenticated GitHub user (injected by Spring Security)
     * @param model      Spring MVC model for passing attributes to the view
     */
    @GetMapping("/plan-trip")
    public String planTrip(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        if (oAuth2User != null) {
            // Expose GitHub user info to the view
            model.addAttribute("username", oAuth2User.getAttribute("login"));
            model.addAttribute("avatarUrl", oAuth2User.getAttribute("avatar_url"));
            model.addAttribute("name",
                    oAuth2User.getAttribute("name") != null
                            ? oAuth2User.getAttribute("name")
                            : oAuth2User.getAttribute("login"));
        }
        return "forward:/plan-trip.html";
    }

    /**
     * GET /dashboard
     * Protected — only authenticated users can reach this page.
     *
     * @param oAuth2User the currently authenticated GitHub user
     * @param model      Spring MVC model
     */
    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User oAuth2User, Model model) {
        if (oAuth2User != null) {
            model.addAttribute("username", oAuth2User.getAttribute("login"));
            model.addAttribute("avatarUrl", oAuth2User.getAttribute("avatar_url"));
            model.addAttribute("name",
                    oAuth2User.getAttribute("name") != null
                            ? oAuth2User.getAttribute("name")
                            : oAuth2User.getAttribute("login"));
        }
        return "forward:/dashboard.html";
    }
}
