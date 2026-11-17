package com.safetrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetrip.model.Trip;
import com.safetrip.model.TripPlace;
import com.safetrip.model.User;
import com.safetrip.repository.TripPlaceRepository;
import com.safetrip.repository.TripRepository;
import com.safetrip.repository.UserRepository;
import com.safetrip.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.frontend-url=http://localhost:5173")
@AutoConfigureMockMvc
class JwtAndUserDataIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripPlaceRepository tripPlaceRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        tripPlaceRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // 1. Email Signup and Login with Validation & BCrypt Verification
    // =========================================================================

    @Test
    void signup_Successful_ReturnsJwtAndHashesPassword() throws Exception {
        Map<String, String> payload = Map.of(
                "email", "alice@example.com",
                "password", "secretPassword123",
                "name", "Alice Wonder"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.name").value("Alice Wonder"))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("alice@example.com", jwtUtil.extractEmail(token));

        // Verify password is encrypted with BCrypt and not stored in plain text
        User savedUser = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertNotEquals("secretPassword123", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("secretPassword123", savedUser.getPassword()));
    }

    @Test
    void signup_DuplicateEmail_ReturnsBadRequest() throws Exception {
        Map<String, String> payload = Map.of(
                "email", "duplicate@example.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // Try registering duplicate
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("An account with this email already exists."));
    }

    @Test
    void signup_InvalidInput_FailsValidation() throws Exception {
        // Short password
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "short@example.com", "password", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password must be at least 6 characters."));

        // Invalid email format
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "not-an-email", "password", "validPass123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Please provide a valid email address."));
    }

    @Test
    void login_Successful_ReturnsValidToken() throws Exception {
        // Register first
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "loginuser@example.com", "password", "correctPass123"))))
                .andExpect(status().isOk());

        // Login with correct credentials
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "loginuser@example.com", "password", "correctPass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value("loginuser@example.com"))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        // Non-existent email
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nonexistent@example.com", "password", "somePass123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }

    // =========================================================================
    // 2. JWT Header Authentication & Private Route Protection
    // =========================================================================

    @Test
    void protectedApi_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/my-places"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void protectedApi_WithValidJwtBearerToken_Succeeds() throws Exception {
        User user = new User();
        user.setEmail("jwtuser@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user);

        mockMvc.perform(get("/api/my-places")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jwtuser@example.com"))
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    void protectedApi_WithMalformedOrInvalidToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/my-places")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 3. User Data Isolation: Users cannot view, update, or delete each other's data
    // =========================================================================

    @Test
    void userDataIsolation_UserACannotAccessUserBData() throws Exception {
        // Create User A
        User userA = new User();
        userA.setEmail("usera@example.com");
        userA.setPassword(passwordEncoder.encode("pass123456"));
        userA = userRepository.save(userA);
        String tokenA = jwtUtil.generateToken(userA);

        // Create User B
        User userB = new User();
        userB.setEmail("userb@example.com");
        userB.setPassword(passwordEncoder.encode("pass123456"));
        userB = userRepository.save(userB);
        String tokenB = jwtUtil.generateToken(userB);

        // User A creates a trip
        Trip tripA = new Trip();
        tripA.setDestination("Paris");
        tripA.setMood("Romantic");
        tripA.setUser(userA);
        tripA = tripRepository.save(tripA);

        // User A creates a place
        TripPlace placeA = new TripPlace();
        placeA.setName("Eiffel Tower");
        placeA.setCategory("Attraction");
        placeA.setLat(48.8584);
        placeA.setLon(2.2945);
        placeA.setUser(userA);
        placeA.setTrip(tripA);
        placeA = tripPlaceRepository.save(placeA);

        // User A sees their trip
        mockMvc.perform(get("/api/trips")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].destination").value("Paris"));

        // User B sees NO trips
        mockMvc.perform(get("/api/trips")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // User A sees their place
        mockMvc.perform(get("/api/my-places")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Eiffel Tower"));

        // User B sees NO places
        mockMvc.perform(get("/api/my-places")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // User B attempts to access User A's trip by ID -> Forbidden
        mockMvc.perform(get("/api/trips/" + tripA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // User B attempts to update User A's trip -> Forbidden
        Trip hackerTrip = new Trip();
        hackerTrip.setDestination("Hacked Destination");
        mockMvc.perform(put("/api/trips/" + tripA.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackerTrip)))
                .andExpect(status().isForbidden());

        // User B attempts to delete User A's trip -> Forbidden
        mockMvc.perform(delete("/api/trips/" + tripA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // User B attempts to delete User A's place -> Forbidden
        mockMvc.perform(delete("/api/my-places/" + placeA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Verify User A's trip and place are still intact
        assertTrue(tripRepository.findById(tripA.getId()).isPresent());
        assertEquals("Paris", tripRepository.findById(tripA.getId()).get().getDestination());
        assertTrue(tripPlaceRepository.findById(placeA.getId()).isPresent());
    }

    // =========================================================================
    // 4. Critical: Google Account A vs Google Account B Isolation
    // =========================================================================

    private OidcUser createGoogleOidcUser(String email, String name, String sub) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("email", email);
        claims.put("name", name);
        claims.put("picture", "https://example.com/avatar.png");
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(), Instant.now().plusSeconds(3600), claims);
        return new DefaultOidcUser(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                idToken,
                "email"
        );
    }

    @Test
    void googleOAuth_AccountsAreIsolatedInDatabase() throws Exception {
        OidcUser googleUserA = createGoogleOidcUser("googleA@gmail.com", "Google User A", "sub-1111");
        OidcUser googleUserB = createGoogleOidcUser("googleB@gmail.com", "Google User B", "sub-2222");

        // Google User A creates a place
        TripPlace placeA = new TripPlace();
        placeA.setName("Louvre Museum");
        placeA.setCategory("Museum");

        mockMvc.perform(post("/api/my-places")
                        .with(SecurityMockMvcRequestPostProcessors.oidcLogin().oidcUser(googleUserA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(placeA)))
                .andExpect(status().isOk());

        // Verify Google User A sees their place
        mockMvc.perform(get("/api/my-places")
                        .with(SecurityMockMvcRequestPostProcessors.oidcLogin().oidcUser(googleUserA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Louvre Museum"));

        // Google User B must see EMPTY places (no leak between Google accounts!)
        mockMvc.perform(get("/api/my-places")
                        .with(SecurityMockMvcRequestPostProcessors.oidcLogin().oidcUser(googleUserB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Check that both users exist in users table with distinct IDs
        User dbUserA = userRepository.findByEmail("googlea@gmail.com").orElseThrow();
        User dbUserB = userRepository.findByEmail("googleb@gmail.com").orElseThrow();
        assertNotEquals(dbUserA.getId(), dbUserB.getId());
    }
}
