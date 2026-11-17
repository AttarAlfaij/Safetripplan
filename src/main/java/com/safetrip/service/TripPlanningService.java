package com.safetrip.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetrip.dto.PlanTripRequest;
import com.safetrip.dto.PlanTripResponse;
import com.safetrip.dto.PlannedPlace;
import com.safetrip.exception.CityNotFoundException;
import com.safetrip.exception.InvalidPlanRequestException;
import com.safetrip.exception.NoPlacesFoundException;
import com.safetrip.model.Trip;
import com.safetrip.model.User;
import com.safetrip.repository.TripPlaceRepository;
import com.safetrip.repository.TripRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TripPlanningService {

    private static final Logger LOG = Logger.getLogger(TripPlanningService.class.getName());

    private static final String USER_AGENT = "SafeTripPlanner/1.0 (https://safetripplanner.com; admin@safetripplanner.com)";

    private static final String[] OVERPASS_MIRRORS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://lz4.overpass-api.de/api/interpreter"
    };

    private static final int MAX_PLACES = 20;

    private static final Set<String> NEGATIVE_NOISE = Set.of(
            "substation", "locomotive shed", "loco shed", "sewage", "public toilet",
            "police station", "jail", "prison", "toll plaza", "petrol pump",
            "gas station", "warehouse", "administrative office", "constituency"
    );

    private final RestTemplate fastRestTemplate;
    private final ObjectMapper objectMapper;
    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final CuratedDestinationRepository curatedRepository;

    private final Map<String, LocationRef> geocodeCache = new ConcurrentHashMap<>();
    private final Map<String, List<PlannedPlace>> placesCache = new ConcurrentHashMap<>();

    public TripPlanningService(ObjectMapper objectMapper,
                               TripRepository tripRepository,
                               TripPlaceRepository tripPlaceRepository,
                               CuratedDestinationRepository curatedRepository) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2500); // 2.5s connect timeout
        factory.setReadTimeout(3500);    // 3.5s read timeout
        this.fastRestTemplate = new RestTemplate(factory);
        this.objectMapper = objectMapper;
        this.tripRepository = tripRepository;
        this.tripPlaceRepository = tripPlaceRepository;
        this.curatedRepository = curatedRepository;
    }

    @Transactional
    public PlanTripResponse planTrip(PlanTripRequest request, User user) {
        validateRequest(request);

        Trip trip = new Trip();
        trip.setDestination(request.getCity());
        trip.setMood(request.getMood());
        if (user != null) {
            trip.setUser(user);
            trip.setGithubId(user.getId().toString());
        }

        Trip savedTrip = tripRepository.save(trip);
        LOG.info("Saved Trip id=" + savedTrip.getId() + (user != null ? " for user=" + user.getId() : ""));

        LocationRef ref = geocodeSafe(request.getCity());

        List<PlannedPlace> places = fetchPlaces(ref, request.getCity(), request.getMood());

        if (places.isEmpty()) {
            throw new NoPlacesFoundException(
                    "No relevant places found for '" + request.getCity()
                            + "' with mood '" + request.getMood()
                            + "'. Try a different mood or a nearby city."
            );
        }

        Map<String, List<PlannedPlace>> itinerary = distributePlacesByDay(places, request.getDays());

        PlanTripResponse response = new PlanTripResponse();
        response.setCity(request.getCity());
        response.setDays(request.getDays());
        response.setItinerary(itinerary);

        return response;
    }

    @Transactional
    public PlanTripResponse planTrip(PlanTripRequest request, String githubId) {
        validateRequest(request);

        Trip trip = new Trip();
        trip.setDestination(request.getCity());
        trip.setMood(request.getMood());
        trip.setGithubId(githubId);

        Trip savedTrip = tripRepository.save(trip);
        LOG.info("Saved Trip id=" + savedTrip.getId());

        LocationRef ref = geocodeSafe(request.getCity());

        List<PlannedPlace> places = fetchPlaces(ref, request.getCity(), request.getMood());

        if (places.isEmpty()) {
            throw new NoPlacesFoundException(
                    "No relevant places found for '" + request.getCity()
                            + "' with mood '" + request.getMood()
                            + "'. Try a different mood or a nearby city."
            );
        }

        Map<String, List<PlannedPlace>> itinerary = distributePlacesByDay(places, request.getDays());

        PlanTripResponse response = new PlanTripResponse();
        response.setCity(request.getCity());
        response.setDays(request.getDays());
        response.setItinerary(itinerary);

        return response;
    }

    private LocationRef geocodeSafe(String location) {
        String cacheKey = location.trim().toLowerCase(Locale.ROOT);
        LocationRef cached = geocodeCache.get(cacheKey);
        if (cached != null) {
            LOG.info("MOOD-TRACE [GEOCODE-CACHE] city='" + location + "' lat=" + cached.lat + " lon=" + cached.lon);
            return cached;
        }

        // 1. Check curated well-known coordinates first (instant 0ms resolution)
        CuratedDestinationRepository.CityCoords coords = curatedRepository.getCoordinates(location);
        if (coords != null) {
            LocationRef ref = new LocationRef(coords.lat, coords.lon);
            geocodeCache.put(cacheKey, ref);
            LOG.info("MOOD-TRACE [GEOCODE-CURATED] city='" + location + "' lat=" + ref.lat + " lon=" + ref.lon);
            return ref;
        }

        // 2. Query Nominatim with proper User-Agent and strict timeout
        String nominatimUrl = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q="
                + URLEncoder.encode(location, StandardCharsets.UTF_8);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept-Language", "en");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            long start = System.currentTimeMillis();
            ResponseEntity<String> response = fastRestTemplate.exchange(nominatimUrl, HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            if (body != null && !body.isBlank()) {
                JsonNode root = objectMapper.readTree(body);
                if (root.isArray() && !root.isEmpty()) {
                    double lat = root.get(0).path("lat").asDouble();
                    double lon = root.get(0).path("lon").asDouble();
                    LocationRef ref = new LocationRef(lat, lon);
                    geocodeCache.put(cacheKey, ref);
                    LOG.info("MOOD-TRACE [GEOCODE-NOMINATIM] city='" + location + "' lat=" + lat + " lon=" + lon
                            + " duration=" + (System.currentTimeMillis() - start) + "ms");
                    return ref;
                }
            }
        } catch (Exception ex) {
            LOG.warning("MOOD-TRACE [GEOCODE-NOMINATIM-FAILED] city='" + location + "': " + ex.getMessage());
        }

        // 3. Fallback to Wikipedia Opensearch Geocoding
        try {
            String wikiGeoUrl = "https://en.wikipedia.org/w/api.php?action=query&prop=coordinates&titles="
                    + URLEncoder.encode(location, StandardCharsets.UTF_8) + "&format=json";
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = fastRestTemplate.exchange(wikiGeoUrl, HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            if (body != null && !body.isBlank()) {
                JsonNode root = objectMapper.readTree(body);
                JsonNode pages = root.path("query").path("pages");
                if (pages.isObject()) {
                    Iterator<JsonNode> it = pages.elements();
                    if (it.hasNext()) {
                        JsonNode page = it.next();
                        JsonNode coordsNode = page.path("coordinates");
                        if (coordsNode.isArray() && !coordsNode.isEmpty()) {
                            double lat = coordsNode.get(0).path("lat").asDouble();
                            double lon = coordsNode.get(0).path("lon").asDouble();
                            LocationRef ref = new LocationRef(lat, lon);
                            geocodeCache.put(cacheKey, ref);
                            LOG.info("MOOD-TRACE [GEOCODE-WIKI] city='" + location + "' lat=" + lat + " lon=" + lon);
                            return ref;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOG.warning("MOOD-TRACE [GEOCODE-WIKI-FAILED] city='" + location + "': " + ex.getMessage());
        }

        throw new CityNotFoundException("City/region not found: " + location);
    }

    private List<PlannedPlace> fetchPlaces(LocationRef ref, String location, String mood) {
        String cacheKey = (location + "::" + mood).toLowerCase(Locale.ROOT);
        List<PlannedPlace> cached = placesCache.get(cacheKey);
        if (cached != null && !cached.isEmpty()) {
            LOG.info("MOOD-TRACE [CACHE-HIT] city='" + location + "' mood='" + mood + "' count=" + cached.size());
            return new ArrayList<>(cached);
        }

        long start = System.currentTimeMillis();
        MoodProfile profile = profileForMood(mood);
        LOG.info("MOOD-TRACE [START] city='" + location + "' mood='" + mood + "' resolvedMood='" + profile.name + "' lat=" + ref.lat + " lon=" + ref.lon);

        List<PlannedPlace> allCandidates = new ArrayList<>();

        // Execute candidate sources concurrently with strict timeout deadline
        CompletableFuture<List<PlannedPlace>> overpassFuture = CompletableFuture.supplyAsync(() -> queryOverpassParallel(ref, profile, location));
        CompletableFuture<List<PlannedPlace>> wikiFuture = CompletableFuture.supplyAsync(() -> queryWikipedia(ref, profile, location));
        CompletableFuture<List<PlannedPlace>> nominatimFuture = CompletableFuture.supplyAsync(() -> queryNominatim(ref, profile, location));

        try {
            CompletableFuture.allOf(overpassFuture, wikiFuture, nominatimFuture)
                    .get(3500, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
            // Proceed immediately with whatever completed within 3.5 seconds
        }

        try {
            if (overpassFuture.isDone() && !overpassFuture.isCompletedExceptionally()) {
                List<PlannedPlace> op = overpassFuture.getNow(Collections.emptyList());
                if (op != null) allCandidates.addAll(op);
            }
        } catch (Exception ignored) {}

        try {
            if (wikiFuture.isDone() && !wikiFuture.isCompletedExceptionally()) {
                List<PlannedPlace> wp = wikiFuture.getNow(Collections.emptyList());
                if (wp != null) allCandidates.addAll(wp);
            }
        } catch (Exception ignored) {}

        try {
            if (nominatimFuture.isDone() && !nominatimFuture.isCompletedExceptionally()) {
                List<PlannedPlace> np = nominatimFuture.getNow(Collections.emptyList());
                if (np != null) allCandidates.addAll(np);
            }
        } catch (Exception ignored) {}

        LOG.info("MOOD-TRACE [ONLINE-RESULTS] city='" + location + "' mood='" + mood + "' rawCollected=" + allCandidates.size()
                + " in " + (System.currentTimeMillis() - start) + "ms");

        // Primary Fallback: If online results yielded fewer than 5 places, enrich with Curated Destination Repository
        if (allCandidates.size() < 5) {
            List<PlannedPlace> curated = curatedRepository.getCuratedPlaces(location, mood);
            if (!curated.isEmpty()) {
                LOG.info("MOOD-TRACE [FALLBACK-CURATED] adding " + curated.size() + " places from curated repository for city='" + location + "' mood='" + mood + "'");
                allCandidates.addAll(curated);
            }
        }

        // Secondary Fallback: If still empty (e.g. obscure city or fully offline environment), generate plausible synthetic places around city center
        if (allCandidates.isEmpty()) {
            List<PlannedPlace> synthetic = curatedRepository.generateSyntheticPlaces(location, mood, ref.lat, ref.lon);
            LOG.info("MOOD-TRACE [FALLBACK-SYNTHETIC] generated " + synthetic.size() + " places for city='" + location + "' mood='" + mood + "'");
            allCandidates.addAll(synthetic);
        }

        // Deduplicate and rank
        List<PlannedPlace> finalPlaces = deduplicateAndRank(allCandidates, profile, location);
        LOG.info("MOOD-TRACE [COMPLETE] city='" + location + "' mood='" + mood + "' finalCount=" + finalPlaces.size()
                + " totalDuration=" + (System.currentTimeMillis() - start) + "ms");

        placesCache.put(cacheKey, finalPlaces);
        return finalPlaces;
    }

    private List<PlannedPlace> queryOverpassParallel(LocationRef ref, MoodProfile profile, String location) {
        long start = System.currentTimeMillis();
        String query = buildLightOverpassQuery(ref, profile, 12000);

        List<CompletableFuture<List<PlannedPlace>>> futures = new ArrayList<>();
        for (String mirror : OVERPASS_MIRRORS) {
            futures.add(CompletableFuture.supplyAsync(() -> querySingleOverpass(mirror, query, profile, location)));
        }

        for (CompletableFuture<List<PlannedPlace>> f : futures) {
            try {
                List<PlannedPlace> result = f.get(2800, TimeUnit.MILLISECONDS);
                if (result != null && !result.isEmpty()) {
                    long duration = System.currentTimeMillis() - start;
                    LOG.info("MOOD-TRACE [OVERPASS] mirror succeeded with " + result.size() + " places in " + duration + "ms");
                    return result;
                }
            } catch (Exception ignored) {
                // Try next future
            }
        }
        LOG.info("MOOD-TRACE [OVERPASS] all mirrors failed or timed out in " + (System.currentTimeMillis() - start) + "ms");
        return Collections.emptyList();
    }

    private List<PlannedPlace> querySingleOverpass(String endpoint, String query, MoodProfile profile, String location) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>("data=" + URLEncoder.encode(query, StandardCharsets.UTF_8), headers);
            String body = fastRestTemplate.postForObject(endpoint, entity, String.class);
            if (body == null || body.isBlank() || body.startsWith("<")) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode elements = root.path("elements");
            if (!elements.isArray() || elements.isEmpty()) {
                return Collections.emptyList();
            }

            List<PlannedPlace> list = new ArrayList<>();
            for (JsonNode e : elements) {
                double lat = e.path("lat").asDouble();
                double lon = e.path("lon").asDouble();
                if (lat == 0.0 && lon == 0.0 && e.has("center")) {
                    lat = e.path("center").path("lat").asDouble();
                    lon = e.path("center").path("lon").asDouble();
                }

                JsonNode tags = e.path("tags");
                String name = tags.path("name").asText("").trim();
                if (name.isEmpty()) continue;

                ScoreResult sr = scorePlace(tags, name, profile);
                if (sr.score < 30) continue;

                PlannedPlace p = new PlannedPlace();
                p.setName(name);
                p.setCategory(resolveCategory(tags));
                p.setLat(lat);
                p.setLon(lon);
                p.setOsmLink("https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon + "#map=16/" + lat + "/" + lon);
                p.setGoogleLink("https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(name + " " + location, StandardCharsets.UTF_8));
                p.setHasWikipedia(tags.hasNonNull("wikipedia") && !tags.path("wikipedia").asText("").isBlank());
                p.setHasTourism(tags.hasNonNull("tourism") && !tags.path("tourism").asText("").isBlank());
                list.add(p);
            }
            return list;
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String buildLightOverpassQuery(LocationRef ref, MoodProfile profile, int radius) {
        String around = "(around:" + radius + "," + ref.lat + "," + ref.lon + ")";
        StringBuilder sb = new StringBuilder();
        // Query node and way only (avoids slow recursive relations)
        for (String[] tag : profile.tags) {
            sb.append("node[\"").append(tag[0]).append("\"=\"").append(tag[1]).append("\"][\"name\"]").append(around).append(";");
            sb.append("way[\"").append(tag[0]).append("\"=\"").append(tag[1]).append("\"][\"name\"]").append(around).append(";");
        }
        return "[out:json][timeout:5];(" + sb + ");out center 40;";
    }

    private List<PlannedPlace> queryWikipedia(LocationRef ref, MoodProfile profile, String location) {
        long start = System.currentTimeMillis();
        try {
            String wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gscoord="
                    + ref.lat + "|" + ref.lon + "&gsradius=15000&gslimit=50&format=json";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = fastRestTemplate.exchange(wikiUrl, HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank()) return Collections.emptyList();

            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("query").path("geosearch");
            if (!items.isArray() || items.isEmpty()) return Collections.emptyList();

            List<PlannedPlace> list = new ArrayList<>();
            for (JsonNode item : items) {
                String title = item.path("title").asText("").trim();
                double lat = item.path("lat").asDouble();
                double lon = item.path("lon").asDouble();
                if (title.isEmpty()) continue;

                ScoreResult sr = scorePlace(null, title, profile);
                if (sr.score < 30) continue;

                PlannedPlace p = new PlannedPlace();
                p.setName(title);
                p.setCategory(resolveCategoryFromTitle(title, profile));
                p.setLat(lat);
                p.setLon(lon);
                p.setOsmLink("https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon + "#map=16/" + lat + "/" + lon);
                p.setGoogleLink("https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(title + " " + location, StandardCharsets.UTF_8));
                p.setHasWikipedia(true);
                p.setHasTourism(true);
                list.add(p);
            }
            LOG.info("MOOD-TRACE [WIKIPEDIA] parsed " + list.size() + " relevant places in " + (System.currentTimeMillis() - start) + "ms");
            return list;
        } catch (Exception ex) {
            LOG.warning("MOOD-TRACE [WIKIPEDIA-FAILED] " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PlannedPlace> queryNominatim(LocationRef ref, MoodProfile profile, String location) {
        long start = System.currentTimeMillis();
        try {
            String queryTerm = profile.strongKeywords.length > 0 ? profile.strongKeywords[0] : "attraction";
            String nomUrl = "https://nominatim.openstreetmap.org/search?q="
                    + URLEncoder.encode(queryTerm + " in " + location, StandardCharsets.UTF_8)
                    + "&format=json&limit=20";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = fastRestTemplate.exchange(nomUrl, HttpMethod.GET, entity, String.class);
            String body = response.getBody();
            if (body == null || body.isBlank() || body.startsWith("<")) return Collections.emptyList();

            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray() || root.isEmpty()) return Collections.emptyList();

            List<PlannedPlace> list = new ArrayList<>();
            for (JsonNode item : root) {
                String name = item.path("name").asText("").trim();
                if (name.isEmpty()) {
                    name = item.path("display_name").asText("").split(",")[0].trim();
                }
                if (name.isEmpty()) continue;

                double lat = item.path("lat").asDouble();
                double lon = item.path("lon").asDouble();

                ScoreResult sr = scorePlace(null, name, profile);
                if (sr.score < 25) continue;

                PlannedPlace p = new PlannedPlace();
                p.setName(name);
                String type = item.path("type").asText("Place");
                p.setCategory(prettify(type));
                p.setLat(lat);
                p.setLon(lon);
                p.setOsmLink("https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon + "#map=16/" + lat + "/" + lon);
                p.setGoogleLink("https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(name + " " + location, StandardCharsets.UTF_8));
                p.setHasTourism(true);
                p.setHasWikipedia(false);
                list.add(p);
            }
            LOG.info("MOOD-TRACE [NOMINATIM] parsed " + list.size() + " places in " + (System.currentTimeMillis() - start) + "ms");
            return list;
        } catch (Exception ex) {
            LOG.warning("MOOD-TRACE [NOMINATIM-FAILED] " + ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PlannedPlace> deduplicateAndRank(List<PlannedPlace> raw, MoodProfile profile, String location) {
        List<ScoredPlace> scoredList = new ArrayList<>();
        Set<String> seenNormNames = new HashSet<>();

        for (PlannedPlace p : raw) {
            if (p == null || p.getName() == null || p.getName().isBlank()) continue;
            String rawName = p.getName().trim();

            // Check negative noise
            String lower = rawName.toLowerCase(Locale.ROOT);
            boolean isNoise = false;
            for (String noise : NEGATIVE_NOISE) {
                if (lower.contains(noise) && !lower.contains("ghat") && !lower.contains("temple") && !lower.contains("church")) {
                    isNoise = true;
                    break;
                }
            }
            if (isNoise) continue;

            String norm = normalizeName(rawName);
            if (norm.isEmpty()) continue;

            // Distance & duplicate check
            boolean duplicate = false;
            for (ScoredPlace existing : scoredList) {
                String existingNorm = normalizeName(existing.place.getName());
                if (norm.equals(existingNorm) || norm.contains(existingNorm) || existingNorm.contains(norm)) {
                    duplicate = true;
                    // If current place has richer metadata, replace
                    if (!existing.place.isHasWikipedia() && p.isHasWikipedia()) {
                        existing.place.setHasWikipedia(true);
                    }
                    break;
                }
                if (p.getLat() != 0.0 && existing.place.getLat() != 0.0) {
                    double dist = distanceMeters(p.getLat(), p.getLon(), existing.place.getLat(), existing.place.getLon());
                    if (dist < 150) { // within 150m
                        duplicate = true;
                        break;
                    }
                }
            }

            if (!duplicate && seenNormNames.add(norm)) {
                ScoreResult sr = scorePlace(null, rawName, profile);
                int score = sr.score;
                if (p.isHasWikipedia()) score += 20;
                if (p.isHasTourism()) score += 10;
                scoredList.add(new ScoredPlace(p, score, sr.reason));
            }
        }

        // Sort by score descending, then hasWikipedia, then alphabetical
        scoredList.sort((a, b) -> {
            int cmp = Integer.compare(b.score, a.score);
            if (cmp != 0) return cmp;
            cmp = Boolean.compare(b.place.isHasWikipedia(), a.place.isHasWikipedia());
            if (cmp != 0) return cmp;
            return a.place.getName().compareToIgnoreCase(b.place.getName());
        });

        List<PlannedPlace> result = new ArrayList<>();
        for (int i = 0; i < scoredList.size() && i < MAX_PLACES; i++) {
            result.add(scoredList.get(i).place);
        }
        return result;
    }

    private ScoreResult scorePlace(JsonNode tags, String name, MoodProfile profile) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (String noise : NEGATIVE_NOISE) {
            if (lowerName.contains(noise) && !lowerName.contains("ghat") && !lowerName.contains("memorial") && !lowerName.contains("temple")) {
                return new ScoreResult(-100, "noise:" + noise);
            }
        }

        int score = 0;
        StringBuilder reason = new StringBuilder();

        // Exact OSM tags matching
        if (tags != null) {
            for (String[] tag : profile.tags) {
                if (tagValueEquals(tags, tag[0], tag[1])) {
                    score += 80;
                    reason.append("exact_tag[").append(tag[0]).append("=").append(tag[1]).append("]");
                    break;
                }
            }
            if (score == 0 && hasAnyKnownTag(tags, profile.tags)) {
                score += 45;
                reason.append("tag_key_match");
            }
        }

        // Strong keyword match
        for (String kw : profile.strongKeywords) {
            if (lowerName.contains(kw)) {
                score += 35;
                if (reason.length() < 120) reason.append("+kw:").append(kw);
                break;
            }
        }

        // Weak keyword match
        for (String kw : profile.weakKeywords) {
            if (lowerName.contains(kw)) {
                score += 15;
                if (reason.length() < 140) reason.append("+wkw:").append(kw);
                break;
            }
        }

        if (tags != null) {
            if (tags.hasNonNull("wikipedia") && !tags.path("wikipedia").asText("").isBlank()) score += 15;
            if (tags.hasNonNull("tourism")) score += 10;
        }

        return new ScoreResult(score, reason.toString());
    }

    private boolean tagValueEquals(JsonNode tags, String key, String value) {
        if (!tags.has(key)) return false;
        String v = tags.path(key).asText("");
        return v.equalsIgnoreCase(value);
    }

    private boolean hasAnyKnownTag(JsonNode tags, String[][] profileTags) {
        for (String[] tag : profileTags) {
            if (tags.has(tag[0])) return true;
        }
        return false;
    }

    private String resolveCategory(JsonNode tags) {
        String[] keys = {"tourism", "amenity", "leisure", "historic", "natural", "building", "shop"};
        for (String k : keys) {
            if (tags.hasNonNull(k)) {
                String v = tags.path(k).asText("");
                if (!v.isBlank()) return prettify(v);
            }
        }
        return "Place";
    }

    private String resolveCategoryFromTitle(String title, MoodProfile profile) {
        String lower = title.toLowerCase(Locale.ROOT);
        if (lower.contains("temple") || lower.contains("mandir")) return "Hindu Temple";
        if (lower.contains("church") || lower.contains("cathedral") || lower.contains("chapel")) return "Christian Church";
        if (lower.contains("mosque") || lower.contains("masjid") || lower.contains("dargah")) return "Islamic Mosque";
        if (lower.contains("gurudwara") || lower.contains("gurdwara")) return "Sikh Gurudwara";
        if (lower.contains("ashram")) return "Spiritual Ashram";
        if (lower.contains("ghat")) return "Sacred Riverfront Ghat";
        if (lower.contains("fort")) return "Historic Fort";
        if (lower.contains("palace")) return "Historic Palace";
        if (lower.contains("museum")) return "Museum";
        if (lower.contains("park")) return "City Park";
        if (lower.contains("garden")) return "Botanical Garden";
        if (lower.contains("zoo")) return "Zoological Park";
        if (lower.contains("waterfall")) return "Waterfall";
        if (lower.contains("lake")) return "Lake";
        if (lower.contains("viewpoint")) return "Scenic Viewpoint";
        return profile.name + " Destination";
    }

    private String prettify(String v) {
        String s = v.replace('_', ' ').trim();
        if (s.isEmpty()) return "Place";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private MoodProfile profileForMood(String mood) {
        String m = mood == null ? "" : mood.trim().toLowerCase(Locale.ROOT);
        switch (m) {
            case "spiritual":
                return new MoodProfile("Spiritual",
                        new String[][]{
                                {"amenity", "place_of_worship"}, {"building", "temple"}, {"building", "church"},
                                {"building", "mosque"}, {"building", "cathedral"}, {"building", "chapel"},
                                {"building", "shrine"}, {"historic", "monastery"}, {"tourism", "attraction"}
                        },
                        new String[]{"temple", "mandir", "mosque", "masjid", "church", "cathedral", "gurudwara",
                                "gurdwara", "shrine", "ashram", "ghat", "monastery", "spiritual", "religious",
                                "dargah", "synagogue", "tirth", "matha", "holy"},
                        new String[]{"worship", "sacred", "prayer", "devotion", "peace"}, 30);
            case "historical":
            case "history":
                return new MoodProfile("Historical",
                        new String[][]{
                                {"historic", "monument"}, {"historic", "castle"}, {"historic", "fort"},
                                {"historic", "ruins"}, {"historic", "memorial"}, {"historic", "archaeological_site"},
                                {"historic", "heritage"}, {"tourism", "museum"}, {"tourism", "attraction"}
                        },
                        new String[]{"fort", "palace", "museum", "monument", "heritage", "ruins", "memorial",
                                "castle", "archaeological", "ancient", "historic", "tomb", "citadel", "mound", "hall"},
                        new String[]{"old", "history", "vintage", "colonial"}, 30);
            case "nature":
                return new MoodProfile("Nature",
                        new String[][]{
                                {"natural", "water"}, {"natural", "waterfall"}, {"natural", "wood"},
                                {"natural", "peak"}, {"natural", "beach"}, {"natural", "wetland"},
                                {"leisure", "nature_reserve"}, {"leisure", "park"}, {"leisure", "garden"},
                                {"boundary", "national_park"}
                        },
                        new String[]{"waterfall", "forest", "lake", "river", "garden", "nature", "park",
                                "sanctuary", "wildlife", "peak", "hill", "valley", "beach", "botanical", "barrage", "woods"},
                        new String[]{"green", "trail", "view", "eco", "scenic"}, 30);
            case "adventure":
                return new MoodProfile("Adventure",
                        new String[][]{
                                {"tourism", "theme_park"}, {"tourism", "attraction"}, {"tourism", "viewpoint"},
                                {"leisure", "water_park"}, {"leisure", "sports_centre"}, {"sport", "climbing"},
                                {"sport", "rafting"}, {"sport", "swimming"}, {"natural", "peak"},
                                {"natural", "cave_entrance"}, {"route", "hiking"}
                        },
                        new String[]{"adventure", "theme park", "water park", "trek", "trekking", "hiking",
                                "climb", "climbing", "rafting", "kayak", "zip", "camp", "camping", "paintball", "safari", "rides"},
                        new String[]{"outdoor", "park", "fun", "action"}, 30);
            case "relaxation":
                return new MoodProfile("Relaxation",
                        new String[][]{
                                {"leisure", "park"}, {"leisure", "garden"}, {"leisure", "spa"},
                                {"leisure", "resort"}, {"natural", "beach"}, {"natural", "water"},
                                {"tourism", "viewpoint"}
                        },
                        new String[]{"garden", "lake", "beach", "spa", "resort", "relax", "peace",
                                "calm", "quiet", "promenade", "retreat", "ghat", "park", "botanical"},
                        new String[]{"view", "water", "lakeside", "breeze"}, 30);
            case "romantic":
                return new MoodProfile("Romantic",
                        new String[][]{
                                {"tourism", "viewpoint"}, {"tourism", "attraction"}, {"natural", "beach"},
                                {"leisure", "garden"}, {"leisure", "park"}, {"amenity", "restaurant"},
                                {"natural", "water"}
                        },
                        new String[]{"sunset", "viewpoint", "lake", "lakeside", "romantic", "garden",
                                "beach", "scenic", "promenade", "waterfront", "terrace", "palace"},
                        new String[]{"cafe", "dining", "park", "walkway"}, 30);
            case "food":
                return new MoodProfile("Food",
                        new String[][]{
                                {"amenity", "restaurant"}, {"amenity", "cafe"}, {"amenity", "fast_food"},
                                {"amenity", "food_court"}, {"amenity", "marketplace"}, {"shop", "bakery"}
                        },
                        new String[]{"restaurant", "cafe", "food", "sweet", "laddu", "kulfi", "chaat",
                                "biryani", "dhaba", "bazaar", "market", "bakery", "dining", "chowk", "cuisine"},
                        new String[]{"eat", "coffee", "tea", "taste", "delicacy"}, 30);
            case "family friendly":
            case "family":
                return new MoodProfile("Family Friendly",
                        new String[][]{
                                {"tourism", "zoo"}, {"tourism", "aquarium"}, {"tourism", "theme_park"},
                                {"tourism", "museum"}, {"leisure", "park"}, {"leisure", "playground"},
                                {"leisure", "garden"}
                        },
                        new String[]{"zoo", "aquarium", "amusement", "theme park", "water park",
                                "science centre", "planetarium", "playground", "park", "museum", "family", "kids", "children", "safari"},
                        new String[]{"fun", "garden", "toy train", "recreation"}, 30);
            default:
                return new MoodProfile("Tourist",
                        new String[][]{
                                {"tourism", "attraction"}, {"tourism", "museum"}, {"tourism", "viewpoint"},
                                {"tourism", "artwork"}, {"historic", "monument"}, {"historic", "memorial"},
                                {"leisure", "park"}
                        },
                        new String[]{"attraction", "landmark", "museum", "viewpoint", "monument",
                                "heritage", "palace", "temple", "fort", "park", "sightseeing", "zoo", "famous"},
                        new String[]{"popular", "city", "central"}, 30);
        }
    }

    private void validateRequest(PlanTripRequest request) {
        if (request == null)
            throw new InvalidPlanRequestException("Request null");

        if (request.getCity() == null || request.getCity().isBlank())
            throw new InvalidPlanRequestException("City required");

        if (request.getMood() == null || request.getMood().isBlank())
            throw new InvalidPlanRequestException("Mood required");

        if (request.getDays() < 1)
            throw new InvalidPlanRequestException("Invalid days");
    }

    private Map<String, List<PlannedPlace>> distributePlacesByDay(List<PlannedPlace> places, int days) {
        Map<String, List<PlannedPlace>> map = new LinkedHashMap<>();
        if (places == null || places.isEmpty()) return map;

        int total = places.size();
        int safeDays = Math.max(1, days);
        int perDay = Math.max(1, (int) Math.ceil((double) total / safeDays));

        int idx = 0;
        for (int i = 0; i < safeDays; i++) {
            List<PlannedPlace> day = new ArrayList<>();
            for (int j = 0; j < perDay && idx < total; j++) {
                day.add(places.get(idx++));
            }
            if (day.isEmpty() && !places.isEmpty()) {
                day.add(places.get(i % places.size()));
            }
            map.put("Day " + (i + 1), day);
        }
        return map;
    }

    public static class LocationRef {
        public final double lat;
        public final double lon;
        public LocationRef(double lat, double lon) { this.lat = lat; this.lon = lon; }
    }

    private static class MoodProfile {
        final String name;
        final String[][] tags;
        final String[] strongKeywords;
        final String[] weakKeywords;
        final int minScore;
        MoodProfile(String name, String[][] tags, String[] strongKeywords, String[] weakKeywords, int minScore) {
            this.name = name;
            this.tags = tags;
            this.strongKeywords = strongKeywords;
            this.weakKeywords = weakKeywords;
            this.minScore = minScore;
        }
    }

    private static class ScoredPlace {
        final PlannedPlace place;
        final int score;
        final String reason;
        ScoredPlace(PlannedPlace place, int score, String reason) {
            this.place = place;
            this.score = score;
            this.reason = reason;
        }
    }

    private static class ScoreResult {
        final int score;
        final String reason;
        ScoreResult(int score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }
}
