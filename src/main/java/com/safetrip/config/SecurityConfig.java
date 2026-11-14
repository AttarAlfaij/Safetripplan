package com.safetrip.config;

import com.safetrip.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // Disable CSRF for now — frontend uses plain fetch() calls without CSRF tokens.
            // (Session-based auth is still used; this is a tradeoff already present in the
            // original app and out of scope for this fix.)
            .csrf(AbstractHttpConfigurer::disable)

            // ── Route authorization rules ───────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Protected React routes & APIs — require an authenticated session
                .requestMatchers("/plan-trip", "/dashboard", "/api/**").authenticated()

                // Public React routes. These return redirects to the frontend;
                // any sensitive data they display still comes from /api/**.
                .requestMatchers("/", "/home", "/about", "/login", "/my-trip", "/risk-map")
                .permitAll()

                // Favicon served by the backend for direct backend navigation
                .requestMatchers("/favicon.ico").permitAll()

                // OAuth2 endpoints must always be reachable, even when logged out
                .requestMatchers("/oauth2/**", "/login/**").permitAll()

                // Everything else defaults to requiring authentication
                .anyRequest().authenticated()
            )

            // ── OAuth2 login (GitHub + Google) ───────────────────────────────────
            // Triggered when something navigates the browser to
            // /oauth2/authorization/github or /oauth2/authorization/google (the
            // "Login with GitHub" / "Continue with Google" buttons). Spring
            // Security does NOT auto-trigger this — see exceptionHandling below.
            .oauth2Login(oauth2 -> oauth2
                // After successful login (either provider), go to /plan-trip.
                // FrontendRedirectController then forwards the browser to the
                // React app, which is served separately from the backend.
                .defaultSuccessUrl("/plan-trip", true)
                // CustomOAuth2UserService links Google logins to the existing
                // "users" table by email (find-or-create). For GitHub it simply
                // delegates to Spring's default behavior, so the GitHub flow is
                // unaffected.
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
            )

            // ── Logout support — clear session and redirect to home ─────────────
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )

            // ── Unauthenticated access handling ─────────────────────────────────
            // IMPORTANT: this is the fix for the "forces GitHub login immediately"
            // bug. We deliberately do NOT redirect unauthenticated users to
            // /oauth2/authorization/github here. GitHub OAuth must only start when
            // the user explicitly clicks the "Login with GitHub" link/button.
            //
            //  - Unauthenticated calls to /api/** (AJAX/fetch) get a plain 401 so
            //    the React AuthContext can treat the user as logged out.
            //  - Unauthenticated browser navigation to a protected page
            //    (/plan-trip, /dashboard) is sent back to the public home page
            //    instead of straight to GitHub.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"error\":\"Not authenticated.\"}");
                    } else {
                        response.sendRedirect("/");
                    }
                })
            );

        return http.build();
    }
}
