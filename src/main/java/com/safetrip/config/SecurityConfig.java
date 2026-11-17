package com.safetrip.config;

import com.safetrip.security.JwtAuthenticationFilter;
import com.safetrip.service.CustomOAuth2UserService;
import com.safetrip.service.CustomOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${frontend.url:}")
    private String frontendUrl;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                          CustomOidcUserService customOidcUserService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    private String getSuccessRedirectUrl() {
        String url = frontendUrl;
        if (url == null || url.isBlank()) {
            url = System.getenv("FRONTEND_URL");
        }
        if (url != null && !url.isBlank()) {
            String trimmed = url.trim().replaceAll("/+$", "");
            return trimmed + "/plan-trip";
        }
        return "/plan-trip";
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]",
                "https://safe-trip-frontend.vercel.app",
                "https://*.vercel.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // Enable CORS with the configured origin rules
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF for API requests and standard operations
            .csrf(AbstractHttpConfigurer::disable)

            // ── Route authorization rules ───────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Allow all preflight CORS OPTIONS requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public auth endpoints
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/signup").permitAll()

                // Favicon served by the backend for direct backend navigation
                .requestMatchers("/favicon.ico").permitAll()

                // Public React routes.
                .requestMatchers("/", "/home", "/about", "/login", "/my-trip", "/risk-map")
                .permitAll()

                // OAuth2 endpoints must always be reachable, even when logged out
                .requestMatchers("/oauth2/**", "/login/**").permitAll()

                // Protected React routes & APIs — require an authenticated session or JWT
                .requestMatchers("/plan-trip", "/dashboard", "/api/**").authenticated()

                // Everything else defaults to requiring authentication
                .anyRequest().authenticated()
            )

            // ── JWT filter ───────────────────────────────────────────────────────
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // ── OAuth2 login (GitHub + Google) ───────────────────────────────────
            // Preserves existing OAuth configuration and redirects
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl(getSuccessRedirectUrl(), true)
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService)
                        .oidcUserService(customOidcUserService)
                )
            )

            // ── Logout support — clear session and redirect to home ─────────────
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )

            // ── Unauthenticated access handling ─────────────────────────────────
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