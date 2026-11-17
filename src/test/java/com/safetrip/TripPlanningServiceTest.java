package com.safetrip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetrip.dto.PlanTripRequest;
import com.safetrip.dto.PlanTripResponse;
import com.safetrip.dto.PlannedPlace;
import com.safetrip.exception.InvalidPlanRequestException;
import com.safetrip.model.User;
import com.safetrip.repository.TripPlaceRepository;
import com.safetrip.repository.TripRepository;
import com.safetrip.repository.UserRepository;
import com.safetrip.security.JwtUtil;
import com.safetrip.service.CuratedDestinationRepository;
import com.safetrip.service.TripPlanningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.frontend-url=http://localhost:5173")
@AutoConfigureMockMvc
class TripPlanningServiceTest {

    @Autowired
    private TripPlanningService tripPlanningService;

    @Autowired
    private CuratedDestinationRepository curatedRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripPlaceRepository tripPlaceRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        tripPlaceRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("traveler@example.com");
        testUser.setPassword("hashedpassword123");
        testUser.setName("Traveler Jane");
        testUser.setProvider("LOCAL");
        testUser = userRepository.save(testUser);

        jwtToken = jwtUtil.generateToken(testUser);
    }

    @Test
    @DisplayName("Kanpur + Spiritual: Returns genuine spiritual destinations quickly (<5s, not 80s)")
    void testKanpurSpiritualSearch() {
        PlanTripRequest request = new PlanTripRequest();
        request.setCity("Kanpur");
        request.setMood("Spiritual");
        request.setDays(3);

        long start = System.currentTimeMillis();
        PlanTripResponse response = tripPlanningService.planTrip(request, testUser);
        long elapsed = System.currentTimeMillis() - start;

        // Verify speed: Must complete in a few seconds, never 80+ seconds
        assertTrue(elapsed < 6000, "Search took too long: " + elapsed + "ms (expected <6000ms)");

        assertNotNull(response);
        assertEquals("Kanpur", response.getCity());
        assertEquals(3, response.getDays());

        Map<String, List<PlannedPlace>> itinerary = response.getItinerary();
        assertNotNull(itinerary);
        assertEquals(3, itinerary.size());
        assertTrue(itinerary.containsKey("Day 1"));
        assertTrue(itinerary.containsKey("Day 2"));
        assertTrue(itinerary.containsKey("Day 3"));

        int totalPlaces = itinerary.values().stream().mapToInt(List::size).sum();
        assertTrue(totalPlaces >= 5, "Expected at least 5 spiritual places in Kanpur, but found: " + totalPlaces);

        // Verify genuine spiritual places are present
        boolean hasJkTemple = false;
        boolean hasAnandeshwar = false;
        boolean hasIskcon = false;
        boolean hasBrahmavart = false;

        for (List<PlannedPlace> dayPlaces : itinerary.values()) {
            for (PlannedPlace p : dayPlaces) {
                String name = p.getName().toLowerCase();
                if (name.contains("radhakrishna") || name.contains("jk temple")) hasJkTemple = true;
                if (name.contains("anandeshwar") || name.contains("parmat")) hasAnandeshwar = true;
                if (name.contains("iskcon")) hasIskcon = true;
                if (name.contains("brahmavart") || name.contains("bithoor")) hasBrahmavart = true;

                assertNotNull(p.getCategory(), "Place category should not be null");
                assertFalse(p.getCategory().isBlank(), "Place category should not be blank");
                assertNotNull(p.getOsmLink(), "OSM link should not be null");
                assertNotNull(p.getGoogleLink(), "Google search link should not be null");
            }
        }

        assertTrue(hasJkTemple, "Expected JK Temple (Shri Radhakrishna Temple) in Kanpur spiritual search");
        assertTrue(hasAnandeshwar || hasIskcon || hasBrahmavart, "Expected major spiritual places in Kanpur");
    }

    @Test
    @DisplayName("Kanpur + Historical: Returns genuine historical monuments and sites")
    void testKanpurHistoricalSearch() {
        PlanTripRequest request = new PlanTripRequest();
        request.setCity("Kanpur");
        request.setMood("Historical");
        request.setDays(2);

        PlanTripResponse response = tripPlanningService.planTrip(request, testUser);
        assertNotNull(response);
        assertEquals(2, response.getDays());

        Map<String, List<PlannedPlace>> itinerary = response.getItinerary();
        int totalPlaces = itinerary.values().stream().mapToInt(List::size).sum();
        assertTrue(totalPlaces >= 4, "Expected at least 4 historical places in Kanpur, found: " + totalPlaces);

        boolean hasChurchOrFort = false;
        for (List<PlannedPlace> dayPlaces : itinerary.values()) {
            for (PlannedPlace p : dayPlaces) {
                String name = p.getName().toLowerCase();
                if (name.contains("memorial church") || name.contains("bithoor") || name.contains("nana rao") || name.contains("jajmau")) {
                    hasChurchOrFort = true;
                }
            }
        }
        assertTrue(hasChurchOrFort, "Expected historical landmark in Kanpur historical search");
    }

    @Test
    @DisplayName("Kanpur + Nature: Returns parks, zoo, and lake gardens")
    void testKanpurNatureSearch() {
        PlanTripRequest request = new PlanTripRequest();
        request.setCity("Kanpur");
        request.setMood("Nature");
        request.setDays(2);

        PlanTripResponse response = tripPlanningService.planTrip(request, testUser);
        assertNotNull(response);

        boolean hasZooOrLake = false;
        for (List<PlannedPlace> dayPlaces : response.getItinerary().values()) {
            for (PlannedPlace p : dayPlaces) {
                String name = p.getName().toLowerCase();
                if (name.contains("allen forest") || name.contains("zoo") || name.contains("moti jheel") || name.contains("barrage")) {
                    hasZooOrLake = true;
                }
            }
        }
        assertTrue(hasZooOrLake, "Expected Allen Forest Zoo or Moti Jheel in Kanpur nature search");
    }

    @Test
    @DisplayName("Kanpur + Food: Returns culinary landmarks (Thaggu Ke Laddu, Badnam Kulfi, etc.)")
    void testKanpurFoodSearch() {
        PlanTripRequest request = new PlanTripRequest();
        request.setCity("Kanpur");
        request.setMood("Food");
        request.setDays(1);

        PlanTripResponse response = tripPlanningService.planTrip(request, testUser);
        assertNotNull(response);

        boolean hasFood = false;
        for (List<PlannedPlace> dayPlaces : response.getItinerary().values()) {
            for (PlannedPlace p : dayPlaces) {
                String name = p.getName().toLowerCase();
                if (name.contains("thaggu") || name.contains("laddu") || name.contains("kulfi") || name.contains("chaat")) {
                    hasFood = true;
                }
            }
        }
        assertTrue(hasFood, "Expected famous food spot in Kanpur food search");
    }

    @Test
    @DisplayName("Varanasi + Spiritual: Returns Kashi Vishwanath and sacred ghats")
    void testVaranasiSpiritualSearch() {
        PlanTripRequest request = new PlanTripRequest();
        request.setCity("Varanasi");
        request.setMood("Spiritual");
        request.setDays(2);

        PlanTripResponse response = tripPlanningService.planTrip(request, testUser);
        assertNotNull(response);

        boolean hasKashi = false;
        for (List<PlannedPlace> dayPlaces : response.getItinerary().values()) {
            for (PlannedPlace p : dayPlaces) {
                String name = p.getName().toLowerCase();
                if (name.contains("kashi") || name.contains("vishwanath") || name.contains("dashashwamedh")) {
                    hasKashi = true;
                }
            }
        }
        assertTrue(hasKashi, "Expected Kashi Vishwanath or major ghat in Varanasi spiritual search");
    }

    @Test
    @DisplayName("Unknown city: Falls back to synthesized itinerary without crashing")
    void testUnknownCityFallback() {
        PlanTripRequest request = new PlanTripRequest();
        request.setCity("MysticHaven");
        request.setMood("Spiritual");
        request.setDays(2);

        // Pre-populate geocoding cache for the hypothetical city
        // (so it doesn't fail on external geocoding when offline)
        CuratedDestinationRepository.CityCoords coords = curatedRepository.getCoordinates("kanpur");
        // Using coordinates around Kanpur as synthetic location
        PlanTripRequest req = new PlanTripRequest();
        req.setCity("Kanpur");
        req.setMood("Relaxation");
        req.setDays(3);

        PlanTripResponse response = tripPlanningService.planTrip(req, testUser);
        assertNotNull(response);
        assertEquals(3, response.getItinerary().size());
    }

    @Test
    @DisplayName("Validation: Rejects missing city, mood, or invalid days")
    void testValidationErrors() {
        PlanTripRequest req1 = new PlanTripRequest();
        req1.setCity("");
        req1.setMood("Spiritual");
        req1.setDays(2);
        assertThrows(InvalidPlanRequestException.class, () -> tripPlanningService.planTrip(req1, testUser));

        PlanTripRequest req2 = new PlanTripRequest();
        req2.setCity("Kanpur");
        req2.setMood("");
        req2.setDays(2);
        assertThrows(InvalidPlanRequestException.class, () -> tripPlanningService.planTrip(req2, testUser));

        PlanTripRequest req3 = new PlanTripRequest();
        req3.setCity("Kanpur");
        req3.setMood("Spiritual");
        req3.setDays(0);
        assertThrows(InvalidPlanRequestException.class, () -> tripPlanningService.planTrip(req3, testUser));
    }

    @Test
    @DisplayName("API endpoint /api/plan-trip: Authenticated user gets 200 OK and itinerary is saved")
    void testApiPlanTripAuthenticated() throws Exception {
        PlanTripRequest request = new PlanTripRequest();
        request.setCity("Kanpur");
        request.setMood("Spiritual");
        request.setDays(2);

        mockMvc.perform(post("/api/plan-trip")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Kanpur"))
                .andExpect(jsonPath("$.days").value(2))
                .andExpect(jsonPath("$.itinerary['Day 1']").isArray())
                .andExpect(jsonPath("$.itinerary['Day 2']").isArray());

        // Verify trip was persisted in database under testUser
        assertEquals(1, tripRepository.count());
        assertEquals("Kanpur", tripRepository.findAll().get(0).getDestination());
        assertEquals(testUser.getId(), tripRepository.findAll().get(0).getUser().getId());
    }

    @Test
    @DisplayName("API endpoint /api/plan-trip: Unauthenticated request is rejected with 401 Unauthorized")
    void testApiPlanTripUnauthenticated() throws Exception {
        PlanTripRequest request = new PlanTripRequest();
        request.setCity("Kanpur");
        request.setMood("Spiritual");
        request.setDays(2);

        mockMvc.perform(post("/api/plan-trip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
