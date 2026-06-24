package com.safetrip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

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
                // Protected pages & APIs — require an authenticated (non-anonymous) session
                .requestMatchers("/plan-trip", "/dashboard", "/api/**").authenticated()

                // Explicit public home routes
                .requestMatchers("/", "/index.html", "/home").permitAll()

                // Other public-facing static pages (not explicitly listed in the
                // requirements, but they are informational pages with no
                // sensitive data of their own — data they display comes from
                // the protected /api/** endpoints, which still require login)
                .requestMatchers("/about.html", "/login.html", "/my-trip.html", "/risk-map.html")
                .permitAll()

                // Static assets (css/js folders + root-level shared assets like
                // navbar.js / navbar.css). These MUST be public — the home page
                // and the navbar itself depend on loading these without
                // authentication, even though some of them are also referenced
                // from protected pages.
                .requestMatchers("/css/**", "/js/**").permitAll()
                .requestMatchers("/*.css", "/*.js", "/*.ico",
                        "/*.png", "/*.jpg", "/*.jpeg", "/*.svg", "/*.webp").permitAll()

                // OAuth2 endpoints must always be reachable, even when logged out
                .requestMatchers("/oauth2/**", "/login/**").permitAll()

                // Everything else defaults to requiring authentication
                .anyRequest().authenticated()
            )

            // ── GitHub OAuth2 login ──────────────────────────────────────────────
            // This is only ever triggered when something navigates the browser to
            // /oauth2/authorization/github (the "Login with GitHub" button/link).
            // Spring Security does NOT auto-trigger this — see exceptionHandling below.
            .oauth2Login(oauth2 -> oauth2
                // After successful GitHub login, go to /plan-trip
                .defaultSuccessUrl("/plan-trip", true)
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
            //    the frontend JS can handle it gracefully (e.g. navbar.js's
            //    fetch('/api/user') just shows the logged-out navbar instead of
            //    blowing up the whole page).
            //  - Unauthenticated browser navigation to a protected page
            //    (/plan-trip, /dashboard) is sent back to the public home page
            //    instead of straight to GitHub.
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/**")
                )
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendRedirect("/")
                )
            );

        return http.build();
    }
}
