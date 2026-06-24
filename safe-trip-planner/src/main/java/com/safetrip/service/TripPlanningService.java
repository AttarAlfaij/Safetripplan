package com.safetrip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetrip.dto.PlanTripRequest;
import com.safetrip.dto.PlanTripResponse;
import com.safetrip.dto.PlannedPlace;
import com.safetrip.exception.CityNotFoundException;
import com.safetrip.exception.InvalidPlanRequestException;
import com.safetrip.exception.NoPlacesFoundException;
import com.safetrip.exception.OverpassApiException;
import com.safetrip.model.Trip;
import com.safetrip.model.TripPlace;
import com.safetrip.repository.TripPlaceRepository;
import com.safetrip.repository.TripRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TripPlanningService {

    private static final Logger LOG = Logger.getLogger(TripPlanningService.class.getName());

    /** Primary Overpass API endpoint */
    private static final String OVERPASS_PRIMARY_URL = "https://overpass-api.de/api/interpreter";

    /** Fallback Overpass API endpoint (mirror) */
    private static final String OVERPASS_FALLBACK_URL = "https://overpass.kumi.systems/api/interpreter";

    /** Radius steps in metres: 10 km → 20 km → 40 km */
    private static final int[] RADIUS_STEPS = { 10_000, 20_000, 40_000 };

    /** Maximum places returned in the itinerary */
    private static final int MAX_PLACES = 20;

    /** Connect timeout in milliseconds */
    private static final int CONNECT_TIMEOUT_MS = 10_000;

    /** Read timeout in milliseconds */
    private static final int READ_TIMEOUT_MS = 45_000;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;

    public TripPlanningService(ObjectMapper objectMapper,
            TripRepository tripRepository,
            TripPlaceRepository tripPlaceRepository) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = objectMapper;
        this.tripRepository = tripRepository;
        this.tripPlaceRepository = tripPlaceRepository;
    }

    // =========================================================
    // Public entry point
    // =========================================================

    @Transactional
    public PlanTripResponse planTrip(PlanTripRequest request, String githubId) {
        validateRequest(request);

        // 1. Save the Trip record FIRST so we have an ID for the FK.
        // Use a separate final-friendly variable so lambdas below can capture it.
        Trip newTrip = new Trip();
        newTrip.setDestination(request.getCity());
        newTrip.setMood(request.getMood());
        newTrip.setGithubId(githubId);
        final Trip savedTrip = tripRepository.save(newTrip); // effectively final
        LOG.info("Saved Trip id=" + savedTrip.getId() + " destination=" + savedTrip.getDestination());

        // 2. Geocode the location name via Nominatim
        LocationRef ref = geocode(request.getCity());

        // 3. Fetch places – tries mood-filtered first, then broad fallback
        List<PlannedPlace> places = fetchPlaces(ref, request.getCity(), request.getMood());

        // 4. Persist places (or fallback dummies) linked to the saved Trip
        if (places.isEmpty()) {
            LOG.warning("Overpass returned no results for \"" + request.getCity()
                    + "\" – inserting fallback dummy places.");
            saveFallbackPlaces(savedTrip, request.getCity(), githubId);
        } else {
            LOG.info("Saving " + places.size() + " places for trip id=" + savedTrip.getId());
            savePlaces(savedTrip, places, githubId);
        }

        // 5. Distribute across days (if Overpass empty, fallback places are in DB but
        // we still surface a clear error to the caller)
        if (places.isEmpty()) {
            throw new NoPlacesFoundException(
                    "No suitable places found for \"" + request.getCity() + "\". Fallback places saved to DB.");
        }

        Map<String, List<PlannedPlace>> itinerary = distributePlacesByDay(places, request.getDays());

        PlanTripResponse response = new PlanTripResponse();
        response.setCity(request.getCity());
        response.setDays(request.getDays());
        response.setItinerary(itinerary);
        return response;
    }

    // =========================================================
    // DB persistence helpers
    // =========================================================

    /** Converts each PlannedPlace to a TripPlace entity and saves it. */
    private void savePlaces(Trip trip, List<PlannedPlace> places, String githubId) {
        for (PlannedPlace p : places) {
            TripPlace tp = new TripPlace();
            tp.setTrip(trip);
            tp.setName(p.getName());
            tp.setCategory(p.getCategory());
            tp.setLat(p.getLat());
            tp.setLon(p.getLon());
            tp.setOsmLink(p.getOsmLink());
            tp.setGithubId(githubId);
            tripPlaceRepository.save(tp);
            LOG.info(() -> "  Saved TripPlace: " + p.getName() + " (trip_id=" + trip.getId() + ")");
        }
    }

    /**
     * Inserts at least 3 dummy TripPlace rows when the API returns nothing.
     * This ensures trip_places is never empty for a saved Trip.
     */
    private void saveFallbackPlaces(Trip trip, String city, String githubId) {
        LOG.warning("[FALLBACK] Inserting 3 default places for city=" + city
                + " trip_id=" + trip.getId());
        String[][] defaults = {
                { "City Centre – " + city, "⭐ Attraction", "0.0", "0.0", "" },
                { "Local Market – " + city, "🗺 Local Market", "0.0", "0.0", "" },
                { "Main Park – " + city, "🌳 Park", "0.0", "0.0", "" }
        };
        for (String[] d : defaults) {
            TripPlace tp = new TripPlace();
            tp.setTrip(trip);
            tp.setName(d[0]);
            tp.setCategory(d[1]);
            tp.setLat(Double.parseDouble(d[2]));
            tp.setLon(Double.parseDouble(d[3]));
            tp.setOsmLink(d[4]);
            tp.setGithubId(githubId);
            tripPlaceRepository.save(tp);
            LOG.warning("[FALLBACK] Saved dummy place: " + d[0]);
        }
    }

    // =========================================================
    // Validation
    // =========================================================

    private void validateRequest(PlanTripRequest request) {
        if (request == null)
            throw new InvalidPlanRequestException("Request body must not be null.");
        if (request.getCity() == null || request.getCity().isBlank())
            throw new InvalidPlanRequestException("City / region name is required.");
        if (request.getMood() == null || request.getMood().isBlank())
            throw new InvalidPlanRequestException("Mood is required.");
        if (request.getDays() < 1)
            throw new InvalidPlanRequestException("Number of days must be at least 1.");

        String mood = request.getMood().trim().toLowerCase(Locale.ROOT);
        Set<String> allowedMoods = Set.of(
                "tourist", "adventure", "historical",
                "romantic", "relaxation", "nature", "relax",
                "spiritual", "family friendly");

        if (!allowedMoods.contains(mood)) {
            throw new InvalidPlanRequestException(
                    "Mood must be one of: Tourist, Adventure, Historical, Romantic, Relaxation, Nature, Relax, Spiritual, Family Friendly.");
        }
    }

    // =========================================================
    // Geocoding via Nominatim
    // =========================================================

    private static class LocationRef {
        final double lat;
        final double lon;

        LocationRef(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    private LocationRef geocode(String location) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?"
                    + "format=json&limit=1&q=" + URLEncoder.encode(location, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SafeTripPlanner/2.0 (contact@safetrip.app)");
            headers.set("Accept-Language", "en");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String body = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
            JsonNode root = objectMapper.readTree(body);

            if (!root.isArray() || root.isEmpty()) {
                throw new CityNotFoundException("Location not found: \"" + location
                        + "\". Please check the spelling or try a broader name (e.g. a state or country).");
            }

            JsonNode first = root.get(0);
            double lat = first.path("lat").asDouble();
            double lon = first.path("lon").asDouble();

            LOG.info(() -> "Geocoded \"" + location + "\" → lat=" + lat + ", lon=" + lon);
            return new LocationRef(lat, lon);

        } catch (IOException | RestClientException ex) {
            throw new OverpassApiException("Failed to geocode location via Nominatim.", ex);
        }
    }

    // =========================================================
    // Place fetching – mood filter + fallback
    // =========================================================

    /**
     * Attempts mood-filtered queries at increasing radii.
     * If all radii fail, retries with broad "all tourism" categories.
     */
    private List<PlannedPlace> fetchPlaces(LocationRef ref, String locationName, String moodRaw) {
        String mood = moodRaw.trim().toLowerCase(Locale.ROOT);

        // --- Pass 1: mood-filtered, escalating radius ---
        for (int radius : RADIUS_STEPS) {
            List<PlannedPlace> places = runOverpassQueryWithRetry(ref, locationName, mood, radius, false);
            if (!places.isEmpty()) {
                LOG.info(() -> "Found " + places.size() + " places with mood filter at radius=" + radius + " m");
                return places;
            }
        }

        // --- Pass 2: broad fallback (all tourism categories, no mood filter) ---
        LOG.warning(() -> "No mood-filtered results – falling back to broad search for \"" + locationName + "\"");
        for (int radius : RADIUS_STEPS) {
            List<PlannedPlace> places = runOverpassQueryWithRetry(ref, locationName, mood, radius, true);
            if (!places.isEmpty()) {
                LOG.info(() -> "Fallback found " + places.size() + " places at radius=" + radius + " m");
                return places;
            }
        }

        return Collections.emptyList();
    }

    // =========================================================
    // Overpass query executor with retry logic
    // =========================================================

    /**
     * Tries the primary Overpass server first. If it fails (timeout, network
     * error, etc.), automatically retries with the fallback mirror. Only throws
     * if both servers fail.
     */
    private List<PlannedPlace> runOverpassQueryWithRetry(LocationRef ref, String locationName,
            String mood, int radius, boolean broadFallback) {

        String filters = broadFallback
                ? buildBroadFilters(ref.lat, ref.lon, radius)
                : buildMoodFilters(mood, ref.lat, ref.lon, radius);

        // node, way, relation – "out center" gives lat/lon for ways/relations
        String query = "[out:json][timeout:45];(" + filters + ");out center " + MAX_PLACES * 2 + ";";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> request = new HttpEntity<>(
                "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8), headers);

        // --- Attempt 1: primary server ---
        String body = null;
        try {
            body = restTemplate.postForObject(OVERPASS_PRIMARY_URL, request, String.class);
        } catch (RestClientException ex) {
            LOG.log(Level.WARNING, "Primary Overpass failed, trying fallback: " + ex.getMessage());
        }

        // --- Attempt 2: fallback server ---
        if (body == null || body.isBlank()) {
            try {
                body = restTemplate.postForObject(OVERPASS_FALLBACK_URL, request, String.class);
            } catch (RestClientException ex) {
                LOG.log(Level.WARNING, "Fallback Overpass also failed: " + ex.getMessage());
                // Return empty instead of throwing – let the caller try a wider radius
                return Collections.emptyList();
            }
        }

        if (body == null || body.isBlank()) {
            return Collections.emptyList();
        }

        try {
            return parseOverpassResponse(body, locationName, mood);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Failed to parse Overpass response: " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    // =========================================================
    // Overpass response parser
    // =========================================================

    private List<PlannedPlace> parseOverpassResponse(String body, String locationName, String mood)
            throws IOException {

        JsonNode root = objectMapper.readTree(body);
        JsonNode elements = root.path("elements");

        if (!elements.isArray() || elements.isEmpty()) {
            return Collections.emptyList();
        }

        List<PlannedPlace> places = new ArrayList<>();
        Set<Long> seenIds = new LinkedHashSet<>();
        Set<String> seenNames = new LinkedHashSet<>();
        Set<String> seenCoords = new LinkedHashSet<>();

        for (JsonNode element : elements) {
            String elemType = element.path("type").asText();

            // Resolve coordinates – node has lat/lon directly; way/relation have a "center"
            double lat, lon;
            if ("node".equals(elemType)) {
                lat = element.path("lat").asDouble();
                lon = element.path("lon").asDouble();
            } else if (element.has("center")) {
                lat = element.path("center").path("lat").asDouble();
                lon = element.path("center").path("lon").asDouble();
            } else {
                continue; // no coordinates → skip
            }

            long id = element.path("id").asLong();
            if (seenIds.contains(id))
                continue;

            JsonNode tags = element.path("tags");
            String name = tags.path("name").asText("").trim();

            // ✓ Requirement 9: Only return results that have a name
            if (name.isEmpty()) {
                continue;
            }

            // ✓ Requirement 8: Deduplicate by name (case-insensitive)
            String nameLower = name.toLowerCase(Locale.ROOT);
            if (seenNames.contains(nameLower))
                continue;

            // ✓ Requirement 8: Deduplicate by lat/lon (rounded to 4 dp ≈ 11m precision)
            String coordKey = Math.round(lat * 10000) + "," + Math.round(lon * 10000);
            if (seenCoords.contains(coordKey))
                continue;

            // Check scoring tags
            boolean hasWikipedia = tags.has("wikipedia");
            boolean hasTourism = tags.has("tourism");

            String category = deriveCategory(tags, mood);

            PlannedPlace place = new PlannedPlace();
            place.setName(name);
            place.setCategory(category);
            place.setLat(lat);
            place.setLon(lon);
            place.setHasWikipedia(hasWikipedia);
            place.setHasTourism(hasTourism);
            place.setOsmLink("https://www.openstreetmap.org/" + elemType + "/" + id);
            place.setGoogleLink("https://www.google.com/search?q="
                    + URLEncoder.encode(name + " " + locationName, StandardCharsets.UTF_8));

            places.add(place);
            seenIds.add(id);
            seenNames.add(nameLower);
            seenCoords.add(coordKey);
        }

        // ✓ Requirement 10: Sort by wikipedia tag → tourism tag → then limit to 20
        places.sort((a, b) -> {
            int aScore = score(a);
            int bScore = score(b);
            return Integer.compare(bScore, aScore); // descending
        });

        // Trim to MAX_PLACES
        if (places.size() > MAX_PLACES) {
            places = new ArrayList<>(places.subList(0, MAX_PLACES));
        }

        return places;
    }

    /** Higher score = shown first */
    private int score(PlannedPlace p) {
        int s = 0;
        // ✓ Requirement 10: prioritise wikipedia, then tourism
        if (p.isHasWikipedia())
            s += 3;
        if (p.isHasTourism())
            s += 2;
        if (p.getName() != null && !p.getName().isBlank())
            s += 1;
        return s;
    }

    // =========================================================
    // Query builders
    // =========================================================

    /**
     * Builds Overpass filters for node, way, and relation elements
     * matching the given mood categories within {@code radius} metres of the
     * centre.
     */
    private String buildMoodFilters(String mood, double lat, double lon, int radius) {
        String around = "(around:" + radius + "," + lat + "," + lon + ")";
        String[] types = { "node", "way", "relation" };
        StringBuilder sb = new StringBuilder();

        String[][] tags = moodTags(mood);
        for (String[] tag : tags) {
            String filter = "[\"" + tag[0] + "\"=\"" + tag[1] + "\"]";
            for (String type : types) {
                sb.append(type).append(filter).append(around).append(";");
            }
        }
        return sb.toString();
    }

    /**
     * Broad fallback – all popular tourism categories using wildcards.
     * ✓ Requirement 6: tourism=*, leisure=*, historic=*, natural=*
     */
    private String buildBroadFilters(double lat, double lon, int radius) {
        String around = "(around:" + radius + "," + lat + "," + lon + ")";
        String[] types = { "node", "way", "relation" };
        String[] broadTags = {
                "[\"tourism\"]",
                "[\"leisure\"]",
                "[\"historic\"]",
                "[\"natural\"]"
        };

        StringBuilder sb = new StringBuilder();
        for (String tag : broadTags) {
            for (String type : types) {
                sb.append(type).append(tag).append(around).append(";");
            }
        }
        return sb.toString();
    }

    /**
     * Maps a mood value to the Overpass key=value tag pairs.
     * ✓ Requirement 5: exact mood-to-tag mapping as specified.
     *
     * Each entry is {key, value} e.g. {"tourism", "viewpoint"}.
     */
    private String[][] moodTags(String mood) {
        switch (mood) {
            case "romantic":
                return new String[][] {
                        { "tourism", "viewpoint" },
                        { "leisure", "park" },
                        { "natural", "water" },
                        { "amenity", "restaurant" }
                };
            case "historical":
                return new String[][] {
                        { "historic", "monument" },
                        { "historic", "castle" },
                        { "historic", "memorial" },
                        { "tourism", "museum" }
                };
            case "family friendly":
                return new String[][] {
                        { "leisure", "park" },
                        { "tourism", "zoo" },
                        { "tourism", "theme_park" },
                        { "amenity", "cinema" }
                };
            case "spiritual":
                return new String[][] {
                        { "amenity", "place_of_worship" },
                        { "historic", "church" },
                        { "historic", "temple" },
                        { "historic", "mosque" }
                };
            case "nature":
                return new String[][] {
                        { "natural", "waterfall" },
                        { "natural", "peak" },
                        { "natural", "forest" },
                        { "leisure", "nature_reserve" }
                };
            case "relaxation":
            case "relax":
                return new String[][] {
                        { "leisure", "park" },
                        { "natural", "water" },
                        { "tourism", "spa" }
                };
            case "tourist":
                return new String[][] {
                        { "tourism", "attraction" },
                        { "tourism", "museum" },
                        { "tourism", "viewpoint" },
                        { "historic", "monument" }
                };
            case "adventure":
                return new String[][] {
                        { "natural", "peak" },
                        { "natural", "waterfall" },
                        { "natural", "cliff" },
                        { "leisure", "park" },
                        { "tourism", "theme_park" }
                };
            default:
                // Fallback to broad tourist categories
                return new String[][] {
                        { "tourism", "attraction" },
                        { "tourism", "viewpoint" },
                        { "tourism", "museum" }
                };
        }
    }

    // =========================================================
    // Category labelling & helpers
    // =========================================================

    private String deriveCategory(JsonNode tags, String mood) {
        if (tags == null || tags.isMissingNode())
            return capitalize(mood);
        if (tags.has("natural")) {
            String val = tags.path("natural").asText();
            return switch (val) {
                case "peak" -> "\uD83C\uDFD4 Mountain Peak";
                case "waterfall" -> "\uD83D\uDCA7 Waterfall";
                case "beach" -> "\uD83C\uDFD6 Beach";
                case "water" -> "\uD83C\uDF0A Water";
                case "lake" -> "\uD83C\uDFDE Lake";
                case "forest" -> "\uD83C\uDF32 Forest";
                default -> "\uD83C\uDF3F Nature – " + capitalize(val);
            };
        }
        if (tags.has("historic")) {
            String val = tags.path("historic").asText();
            return "\uD83C\uDFDB " + capitalize(val.replace('_', ' '));
        }
        if (tags.has("tourism")) {
            String val = tags.path("tourism").asText();
            return switch (val) {
                case "attraction" -> "\u2B50 Attraction";
                case "viewpoint" -> "\uD83D\uDDBC Viewpoint";
                case "museum" -> "\uD83C\uDFDB Museum";
                case "hotel" -> "\uD83C\uDFE8 Hotel";
                case "theme_park" -> "\uD83C\uDFA1 Theme Park";
                case "zoo" -> "\uD83E\uDD92 Zoo";
                case "aquarium" -> "\uD83D\uDC20 Aquarium";
                case "spa" -> "\u2668 Spa";
                default -> "\uD83D\uDDFA " + capitalize(val.replace('_', ' '));
            };
        }
        if (tags.has("leisure")) {
            String val = tags.path("leisure").asText();
            return switch (val) {
                case "park" -> "\uD83C\uDF33 Park";
                case "garden" -> "\uD83C\uDF39 Garden";
                case "nature_reserve" -> "\uD83C\uDF3F Nature Reserve";
                case "playground" -> "\uD83C\uDFA0 Playground";
                default -> "\uD83C\uDFAF " + capitalize(val.replace('_', ' '));
            };
        }
        if (tags.has("amenity")) {
            String val = tags.path("amenity").asText();
            return switch (val) {
                case "place_of_worship" -> "\uD83D\uDD4C Place of Worship";
                case "restaurant" -> "\uD83C\uDF7D Restaurant";
                case "cinema" -> "\uD83C\uDFAC Cinema";
                default -> "\uD83D\uDCCD " + capitalize(val.replace('_', ' '));
            };
        }
        return capitalize(mood);
    }

    // =========================================================
    // Day distribution
    // =========================================================

    private Map<String, List<PlannedPlace>> distributePlacesByDay(List<PlannedPlace> places, int days) {
        Map<String, List<PlannedPlace>> itinerary = new LinkedHashMap<>();
        int total = places.size();
        int basePerDay = total / days;
        int remainder = total % days;
        int index = 0;

        for (int i = 0; i < days; i++) {
            int itemsForDay = basePerDay + (i < remainder ? 1 : 0);
            List<PlannedPlace> dayPlaces = new ArrayList<>();
            for (int j = 0; j < itemsForDay && index < total; j++) {
                dayPlaces.add(places.get(index++));
            }
            itinerary.put("Day " + (i + 1), dayPlaces);
        }
        return itinerary;
    }

    // =========================================================
    // String utilities
    // =========================================================

    private String capitalize(String value) {
        if (value == null || value.isEmpty())
            return value;
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
