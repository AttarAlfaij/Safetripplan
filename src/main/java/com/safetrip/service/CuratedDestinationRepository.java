package com.safetrip.service;

import com.safetrip.dto.PlannedPlace;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class CuratedDestinationRepository {

    public static class CityCoords {
        public final double lat;
        public final double lon;
        public final String displayName;

        public CityCoords(double lat, double lon, String displayName) {
            this.lat = lat;
            this.lon = lon;
            this.displayName = displayName;
        }
    }

    private static final Map<String, CityCoords> CITY_COORDS = new HashMap<>();
    private static final Map<String, List<PlannedPlace>> CURATED_PLACES = new HashMap<>();

    static {
        // Known City Coordinates (instant lookup without slow network)
        addCity("kanpur", 26.4499, 80.3319, "Kanpur, Uttar Pradesh, India");
        addCity("varanasi", 25.3176, 82.9739, "Varanasi, Uttar Pradesh, India");
        addCity("banaras", 25.3176, 82.9739, "Varanasi, Uttar Pradesh, India");
        addCity("kashi", 25.3176, 82.9739, "Varanasi, Uttar Pradesh, India");
        addCity("lucknow", 26.8467, 80.9462, "Lucknow, Uttar Pradesh, India");
        addCity("agra", 27.1767, 78.0081, "Agra, Uttar Pradesh, India");
        addCity("delhi", 28.6139, 77.2090, "New Delhi, Delhi, India");
        addCity("new delhi", 28.6139, 77.2090, "New Delhi, Delhi, India");
        addCity("jaipur", 26.9124, 75.7873, "Jaipur, Rajasthan, India");
        addCity("mumbai", 19.0760, 72.8777, "Mumbai, Maharashtra, India");
        addCity("bombay", 19.0760, 72.8777, "Mumbai, Maharashtra, India");
        addCity("bengaluru", 12.9716, 77.5946, "Bengaluru, Karnataka, India");
        addCity("bangalore", 12.9716, 77.5946, "Bengaluru, Karnataka, India");
        addCity("kolkata", 22.5726, 88.3639, "Kolkata, West Bengal, India");
        addCity("calcutta", 22.5726, 88.3639, "Kolkata, West Bengal, India");
        addCity("chennai", 13.0827, 80.2707, "Chennai, Tamil Nadu, India");
        addCity("madras", 13.0827, 80.2707, "Chennai, Tamil Nadu, India");
        addCity("hyderabad", 17.3850, 78.4867, "Hyderabad, Telangana, India");
        addCity("goa", 15.2993, 74.1240, "Goa, India");
        addCity("panaji", 15.4909, 73.8278, "Panaji, Goa, India");
        addCity("amritsar", 31.6340, 74.8723, "Amritsar, Punjab, India");
        addCity("rishikesh", 30.0869, 78.2676, "Rishikesh, Uttarakhand, India");
        addCity("haridwar", 29.9457, 78.1642, "Haridwar, Uttarakhand, India");
        addCity("shimla", 31.1048, 77.1734, "Shimla, Himachal Pradesh, India");
        addCity("manali", 32.2432, 77.1892, "Manali, Himachal Pradesh, India");
        addCity("srinagar", 34.0837, 74.7973, "Srinagar, Jammu & Kashmir, India");
        addCity("kashmir", 34.0837, 74.7973, "Srinagar, Jammu & Kashmir, India");
        addCity("leh", 34.1526, 77.5771, "Leh, Ladakh, India");
        addCity("ladakh", 34.1526, 77.5771, "Leh, Ladakh, India");
        addCity("pune", 18.5204, 73.8567, "Pune, Maharashtra, India");
        addCity("ahmedabad", 23.0225, 72.5714, "Ahmedabad, Gujarat, India");
        addCity("kochi", 9.9312, 76.2673, "Kochi, Kerala, India");
        addCity("cochin", 9.9312, 76.2673, "Kochi, Kerala, India");
        addCity("paris", 48.8566, 2.3522, "Paris, France");
        addCity("london", 51.5074, -0.1278, "London, United Kingdom");
        addCity("rome", 41.9028, 12.4964, "Rome, Italy");
        addCity("tokyo", 35.6762, 139.6503, "Tokyo, Japan");
        addCity("new york", 40.7128, -74.0060, "New York, USA");
        addCity("dubai", 25.2048, 55.2708, "Dubai, United Arab Emirates");

        // Curated Kanpur Places
        initKanpurPlaces();

        // Curated Varanasi Places
        initVaranasiPlaces();

        // Curated Delhi Places
        initDelhiPlaces();

        // Curated Jaipur Places
        initJaipurPlaces();

        // Curated Agra Places
        initAgraPlaces();

        // Curated Goa Places
        initGoaPlaces();
    }

    private static void addCity(String name, double lat, double lon, String displayName) {
        CITY_COORDS.put(name.toLowerCase(Locale.ROOT), new CityCoords(lat, lon, displayName));
    }

    public CityCoords getCoordinates(String city) {
        if (city == null) return null;
        String clean = city.trim().toLowerCase(Locale.ROOT);
        if (CITY_COORDS.containsKey(clean)) {
            return CITY_COORDS.get(clean);
        }
        for (Map.Entry<String, CityCoords> entry : CITY_COORDS.entrySet()) {
            if (clean.contains(entry.getKey()) || entry.getKey().contains(clean)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public List<PlannedPlace> getCuratedPlaces(String city, String mood) {
        if (city == null || mood == null) return Collections.emptyList();
        String key = city.trim().toLowerCase(Locale.ROOT) + "::" + mood.trim().toLowerCase(Locale.ROOT);
        List<PlannedPlace> exact = CURATED_PLACES.get(key);
        if (exact != null && !exact.isEmpty()) {
            return new ArrayList<>(exact);
        }

        // Partial match
        for (Map.Entry<String, List<PlannedPlace>> entry : CURATED_PLACES.entrySet()) {
            String[] parts = entry.getKey().split("::");
            if (parts.length == 2) {
                String c = parts[0];
                String m = parts[1];
                if ((city.toLowerCase(Locale.ROOT).contains(c) || c.contains(city.toLowerCase(Locale.ROOT)))
                        && (mood.toLowerCase(Locale.ROOT).contains(m) || m.contains(mood.toLowerCase(Locale.ROOT)))) {
                    return new ArrayList<>(entry.getValue());
                }
            }
        }
        return Collections.emptyList();
    }

    public List<PlannedPlace> generateSyntheticPlaces(String city, String mood, double lat, double lon) {
        List<PlannedPlace> list = new ArrayList<>();
        String m = mood.toLowerCase(Locale.ROOT);

        String[][] templates;
        if (m.contains("spiritual")) {
            templates = new String[][]{
                    {city + " Historic Temple", "Hindu Temple", "0.005", "0.004"},
                    {"Shri Ram & Krishna Mandir, " + city, "Temple & Sacred Shrine", "-0.006", "0.003"},
                    {city + " Central Cathedral & Church", "Christian Church", "0.003", "-0.007"},
                    {city + " Jama Masjid & Dargah", "Mosque & Heritage", "-0.004", "-0.005"},
                    {"Gurudwara Sahib " + city, "Sikh Gurudwara", "0.008", "0.002"},
                    {"Shri Hanuman Mandir, " + city, "Pilgrimage Shrine", "-0.007", "0.008"},
                    {city + " Riverside Ghats & Ashram", "Sacred Ghat & Retreat", "0.012", "-0.003"},
                    {"Shiv Mandir & Spiritual Park, " + city, "Spiritual Landmark", "0.001", "0.011"},
                    {city + " Meditation & Yoga Ashram", "Ashram & Wellness", "-0.011", "-0.006"},
                    {"Maa Durga Bhavani Temple", "Revered Shrine", "0.006", "-0.010"}
            };
        } else if (m.contains("historical") || m.contains("history")) {
            templates = new String[][]{
                    {city + " Heritage Fort & Citadel", "Historic Fort", "0.007", "0.005"},
                    {city + " State Museum & Gallery", "Museum", "-0.005", "0.003"},
                    {"Memorial Clock Tower & Old City", "Historic Monument", "0.002", "-0.006"},
                    {city + " Colonial Heritage Hall", "Historic Landmark", "-0.008", "-0.004"},
                    {"Ancient Archaeological Site & Ruins", "Archaeological Site", "0.012", "0.008"},
                    {"King's Palace & Courtyard, " + city, "Heritage Palace", "-0.006", "0.011"},
                    {city + " Freedom Fighters Memorial", "Historical Memorial", "0.009", "-0.007"},
                    {"Old Royal Residency & Gardens", "Historical Site", "-0.010", "-0.008"}
            };
        } else if (m.contains("nature")) {
            templates = new String[][]{
                    {city + " Botanical Gardens", "Botanical Garden", "0.006", "0.005"},
                    {city + " Lake & Eco Park", "Lake & Nature Park", "-0.008", "0.004"},
                    {"Green Valley Nature Reserve", "Nature Reserve", "0.015", "-0.010"},
                    {"Riverside Sunset Walkway, " + city, "Riverside Promenade", "0.003", "-0.007"},
                    {"Hilltop Scenic Viewpoint", "Viewpoint", "-0.012", "0.014"},
                    {city + " Bird Sanctuary & Forest Trails", "Wildlife & Woods", "0.018", "0.006"},
                    {"Central Forest & Deer Park", "Nature Park", "-0.005", "-0.009"}
            };
        } else if (m.contains("adventure")) {
            templates = new String[][]{
                    {city + " Adventure & Theme Park", "Amusement & Rides", "0.010", "0.008"},
                    {city + " Outdoor Trek & Hiking Trail", "Hiking Trail", "0.020", "-0.015"},
                    {"Aqua Splash Water Park, " + city, "Water Park", "-0.012", "0.010"},
                    {city + " Paintball & Tactical Arena", "Adventure Sports", "0.005", "-0.008"},
                    {"Riverside Kayaking & Boating Point", "Water Adventure", "0.014", "-0.006"},
                    {"Rock Climbing & Rope Course Club", "Climbing & Adventure", "-0.018", "-0.012"}
            };
        } else if (m.contains("food")) {
            templates = new String[][]{
                    {city + " Famous Heritage Sweet Shop", "Traditional Sweets", "0.002", "0.003"},
                    {"Old Bazaar Street Food Street", "Street Food Market", "-0.004", "0.002"},
                    {city + " Chaat & Snacks Chowk", "Chaat & Street Delicacy", "0.003", "-0.004"},
                    {"Royal Biryani & Mughlai Court", "Heritage Restaurant", "-0.005", "-0.003"},
                    {"Central Tea Stall & Bun Maska Hub", "Traditional Cafe", "0.006", "0.001"},
                    {"Baker Street Bakery & Confectionery", "Bakery & Desserts", "-0.002", "0.006"}
            };
        } else if (m.contains("relaxation")) {
            templates = new String[][]{
                    {city + " Serene Lakeside Gardens", "Lake & Park", "0.005", "0.004"},
                    {city + " Botanical Retreat & Spa", "Wellness & Gardens", "-0.006", "0.005"},
                    {"Buddha Peace Park, " + city, "Tranquil Park", "0.008", "-0.007"},
                    {"Riverside Promenade & Ghat", "Calm Scenic Walk", "0.011", "-0.004"},
                    {"Sunset Lawn & Quiet Meadows", "Open Green Space", "-0.009", "-0.006"}
            };
        } else if (m.contains("romantic")) {
            templates = new String[][]{
                    {city + " Lakeside Sunset Promenade", "Scenic Promenade", "0.005", "0.006"},
                    {"Panoramic Riverside Viewpoint", "Romantic Viewpoint", "0.010", "-0.005"},
                    {"Rose & Floral Gardens, " + city, "Botanical Gardens", "-0.006", "0.004"},
                    {"Candlelight Heritage Terrace Cafe", "Scenic Dining", "0.002", "-0.003"},
                    {"Starlight Waterfront Walk", "Riverside Walkway", "-0.008", "-0.007"}
            };
        } else if (m.contains("family")) {
            templates = new String[][]{
                    {city + " Zoological Park & Safari", "Zoo & Wildlife", "0.008", "0.007"},
                    {city + " Amusement & Water Park", "Theme Park", "-0.010", "0.009"},
                    {"Children's Fun Lake & Boating Park", "Recreation Park", "0.004", "-0.005"},
                    {city + " Science Centre & Planetarium", "Science Museum", "-0.006", "-0.004"},
                    {"Public Green Park & Toy Train", "Family Park", "0.007", "-0.008"}
            };
        } else {
            // Tourist / Default
            templates = new String[][]{
                    {city + " Historic Landmark & Monument", "Top Attraction", "0.004", "0.003"},
                    {city + " Famous Heritage Temple / Shrine", "Heritage Landmark", "-0.005", "0.004"},
                    {city + " Central City Park & Lake", "City Park", "0.007", "-0.006"},
                    {city + " Memorial & Cultural Centre", "Cultural Landmark", "-0.007", "-0.005"},
                    {city + " Popular Scenic Viewpoint", "Scenic Viewpoint", "0.012", "-0.003"},
                    {city + " Heritage Museum & Gallery", "Museum", "-0.003", "0.008"},
                    {"Old Town Bazaar & Craft Market", "Tourist Market", "0.002", "-0.004"}
            };
        }

        for (String[] t : templates) {
            double pLat = lat + Double.parseDouble(t[2]);
            double pLon = lon + Double.parseDouble(t[3]);
            PlannedPlace p = new PlannedPlace();
            p.setName(t[0]);
            p.setCategory(t[1]);
            p.setLat(pLat);
            p.setLon(pLon);
            p.setOsmLink(buildOsmLink(pLat, pLon));
            p.setGoogleLink(buildGoogleLink(t[0], city));
            p.setHasTourism(true);
            p.setHasWikipedia(false);
            list.add(p);
        }
        return list;
    }

    private static PlannedPlace place(String name, String category, double lat, double lon, String city, boolean wiki) {
        PlannedPlace p = new PlannedPlace();
        p.setName(name);
        p.setCategory(category);
        p.setLat(lat);
        p.setLon(lon);
        p.setOsmLink(buildOsmLink(lat, lon));
        p.setGoogleLink(buildGoogleLink(name, city));
        p.setHasWikipedia(wiki);
        p.setHasTourism(true);
        return p;
    }

    private static String buildOsmLink(double lat, double lon) {
        return "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon + "#map=16/" + lat + "/" + lon;
    }

    private static String buildGoogleLink(String name, String city) {
        return "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(name + " " + city, StandardCharsets.UTF_8);
    }

    private static void initKanpurPlaces() {
        String city = "Kanpur";
        // Spiritual
        List<PlannedPlace> spiritual = new ArrayList<>();
        spiritual.add(place("Shri Radhakrishna Temple (JK Temple)", "Hindu Temple", 26.4735, 80.3060, city, true));
        spiritual.add(place("Anandeshwar Temple (Parmat)", "Historic Shiva Temple", 26.4840, 80.3450, city, true));
        spiritual.add(place("ISKCON Temple Kanpur", "Vedic Temple & Cultural Center", 26.4950, 80.2650, city, true));
        spiritual.add(place("Bithoor Brahmavart Ghat", "Sacred Pilgrimage Ghat", 26.6160, 80.2740, city, true));
        spiritual.add(place("Kanpur Memorial Church (All Souls)", "Historic Gothic Church", 26.4560, 80.3630, city, true));
        spiritual.add(place("Tapeshwari Devi Temple", "Historic Shakti Shrine", 26.4620, 80.3340, city, true));
        spiritual.add(place("Panki Hanuman Temple", "Ancient Pilgrimage Temple", 26.4820, 80.2480, city, true));
        spiritual.add(place("Baradevi Temple", "Historic Mandir", 26.4380, 80.3310, city, true));
        spiritual.add(place("Valmiki Ashram (Bithoor)", "Historic Vedic Ashram", 26.6180, 80.2710, city, true));
        spiritual.add(place("Kherepati Temple", "Historic Central Temple", 26.4670, 80.3420, city, false));
        spiritual.add(place("Siddhadham Ashram (Jajmau)", "Spiritual Ashram & Retreat", 26.4250, 80.4020, city, false));
        CURATED_PLACES.put("kanpur::spiritual", spiritual);

        // Historical
        List<PlannedPlace> historical = new ArrayList<>();
        historical.add(place("Kanpur Memorial Church", "Historic 1875 Memorial Site", 26.4560, 80.3630, city, true));
        historical.add(place("Bithoor Fort & Valmiki Ashram", "1857 Independence Epicenter", 26.6160, 80.2740, city, true));
        historical.add(place("Massacre Ghat (Satti Chaura)", "Historic 1857 Memorial Ghat", 26.4710, 80.3680, city, true));
        historical.add(place("Nana Rao Park & Memorial", "Historical Freedom Fighters Park", 26.4720, 80.3540, city, true));
        historical.add(place("Jajmau Archaeological Mound", "Ancient 600 BCE Heritage Site", 26.4270, 80.4070, city, true));
        historical.add(place("Phool Bagh & Ganesh Shankar Vidyarthi Hall", "Colonial Heritage Hall", 26.4700, 80.3520, city, true));
        historical.add(place("King Edward Memorial Hall (KEM Hall)", "British Heritage Architecture", 26.4690, 80.3510, city, false));
        historical.add(place("Lal Imli Historic Mills Heritage Area", "Industrial Heritage Landmark", 26.4650, 80.3560, city, false));
        CURATED_PLACES.put("kanpur::historical", historical);

        // Nature
        List<PlannedPlace> nature = new ArrayList<>();
        nature.add(place("Allen Forest Zoo (Kanpur Zoological Park)", "Natural Forest & Zoo", 26.4980, 80.2970, city, true));
        nature.add(place("Moti Jheel & Lake Gardens", "Lakeside Botanical Gardens", 26.4790, 80.3150, city, true));
        nature.add(place("Nana Rao Park", "Tranquil Historic Gardens", 26.4720, 80.3540, city, true));
        nature.add(place("Buddha Park (Indira Nagar)", "Nature & Peace Park", 26.5050, 80.2890, city, false));
        nature.add(place("Ganga Barrage (Lav Kush Barrage)", "Riverside Scenic Viewpoint", 26.5160, 80.3160, city, true));
        nature.add(place("Phool Bagh Botanical Gardens", "Historic Botanical Gardens", 26.4700, 80.3520, city, true));
        nature.add(place("Bithoor Riverside Promenade", "Scenic Ganga Riverfront", 26.6150, 80.2760, city, true));
        CURATED_PLACES.put("kanpur::nature", nature);

        // Relaxation
        List<PlannedPlace> relaxation = new ArrayList<>(nature);
        CURATED_PLACES.put("kanpur::relaxation", relaxation);

        // Adventure
        List<PlannedPlace> adventure = new ArrayList<>();
        adventure.add(place("Blue World Theme Park", "Amusement & Water Park", 26.5700, 80.2100, city, true));
        adventure.add(place("Kanpur Sports Village & Adventure Zone", "Sports & Adventure Arena", 26.5020, 80.2850, city, false));
        adventure.add(place("Ganga Boating & Kayaking Point Bithoor", "River Boating & Water Sports", 26.6150, 80.2770, city, false));
        adventure.add(place("Allen Forest Nature Trails", "Forest Canopy Trekking", 26.4990, 80.2950, city, false));
        adventure.add(place("Extreme Paintball & Adventure Arena", "Tactical Games Arena", 26.4850, 80.2600, city, false));
        CURATED_PLACES.put("kanpur::adventure", adventure);

        // Food
        List<PlannedPlace> food = new ArrayList<>();
        food.add(place("Thaggu Ke Laddu (Bada Chauraha)", "Famous Pure Mawa Laddus", 26.4640, 80.3500, city, true));
        food.add(place("Badnam Kulfi & Rabri House", "Iconic Traditional Desserts", 26.4645, 80.3505, city, true));
        food.add(place("Chaman Ganj Food Street", "Mughlai, Biryani & Kebabs", 26.4660, 80.3330, city, true));
        food.add(place("Naveen Market Chaat Hub", "Famous Spicy Chaat & Snacks", 26.4670, 80.3470, city, false));
        food.add(place("Banarasi Tea Stall & Bun Maska", "Historic Tea Shop", 26.4680, 80.3490, city, false));
        food.add(place("Pandit Ji Ki Chaat (Meston Road)", "Historic Chaat Institution", 26.4650, 80.3480, city, false));
        CURATED_PLACES.put("kanpur::food", food);

        // Romantic
        List<PlannedPlace> romantic = new ArrayList<>();
        romantic.add(place("Moti Jheel Lakeside Promenade", "Scenic Illuminated Lake", 26.4790, 80.3150, city, true));
        romantic.add(place("Ganga Barrage Sunset Point", "Panoramic Sunset Viewpoint", 26.5160, 80.3160, city, true));
        romantic.add(place("Bithoor Riverside Sunset Ghat", "Peaceful Ganga Riverfront", 26.6160, 80.2740, city, true));
        romantic.add(place("Buddha Park Quiet Walkways", "Shaded Scenic Pathways", 26.5050, 80.2890, city, false));
        romantic.add(place("The Deck Riverside Lounge & Cafe", "Riverside Dining & View", 26.4800, 80.3560, city, false));
        CURATED_PLACES.put("kanpur::romantic", romantic);

        // Family Friendly
        List<PlannedPlace> family = new ArrayList<>();
        family.add(place("Allen Forest Zoo & Safari", "Wildlife Exhibits & Toy Train", 26.4980, 80.2970, city, true));
        family.add(place("Blue World Theme & Water Park", "Family Entertainment & Rides", 26.5700, 80.2100, city, true));
        family.add(place("Moti Jheel Children's Park & Boating", "Boating & Playgrounds", 26.4790, 80.3150, city, true));
        family.add(place("Kanpur Science Centre & Planetarium", "Interactive Science Exhibits", 26.4860, 80.2910, city, false));
        family.add(place("Nana Rao Park Children's Zone", "Open Greenery & Play Area", 26.4720, 80.3540, city, true));
        CURATED_PLACES.put("kanpur::family friendly", family);
        CURATED_PLACES.put("kanpur::family", family);

        // Tourist
        List<PlannedPlace> tourist = new ArrayList<>();
        tourist.add(place("Shri Radhakrishna Temple (JK Temple)", "Iconic White Marble Temple", 26.4735, 80.3060, city, true));
        tourist.add(place("Allen Forest Zoo", "Asia's Largest Zoo Sanctuary", 26.4980, 80.2970, city, true));
        tourist.add(place("Kanpur Memorial Church", "Historic Gothic Memorial", 26.4560, 80.3630, city, true));
        tourist.add(place("Moti Jheel & Lake Promenade", "Central Lake & Gardens", 26.4790, 80.3150, city, true));
        tourist.add(place("Bithoor Brahmavart Ghat", "Sacred Ganga Ghat", 26.6160, 80.2740, city, true));
        tourist.add(place("Nana Rao Park", "Historic Freedom Fighters Memorial", 26.4720, 80.3540, city, true));
        tourist.add(place("Blue World Theme Park", "Major Amusement Destination", 26.5700, 80.2100, city, true));
        tourist.add(place("Anandeshwar Temple (Parmat)", "Ancient Shiva Pilgrimage", 26.4840, 80.3450, city, true));
        CURATED_PLACES.put("kanpur::tourist", tourist);
    }

    private static void initVaranasiPlaces() {
        String city = "Varanasi";
        List<PlannedPlace> spiritual = new ArrayList<>();
        spiritual.add(place("Kashi Vishwanath Temple", "Jyotirlinga Hindu Temple", 25.3109, 83.0107, city, true));
        spiritual.add(place("Dashashwamedh Ghat", "Sacred Ganga Aarti Ghat", 25.3069, 83.0104, city, true));
        spiritual.add(place("Sankat Mochan Hanuman Temple", "Ancient Pilgrimage Temple", 25.2818, 82.9995, city, true));
        spiritual.add(place("Assi Ghat", "Spiritual Sunrise Ghat", 25.2906, 83.0068, city, true));
        spiritual.add(place("Sarnath Dhamek Stupa", "Buddhist Sacred Pilgrimage", 25.3809, 83.0245, city, true));
        spiritual.add(place("Manikarnika Ghat", "Sacred Cremation Ghat", 25.3112, 83.0142, city, true));
        spiritual.add(place("Tulsi Manas Temple", "Historic Ramayana Temple", 25.2872, 83.0006, city, true));
        spiritual.add(place("Durga Kund Temple", "Red Stone Historic Temple", 25.2894, 83.0019, city, true));
        CURATED_PLACES.put("varanasi::spiritual", spiritual);
        CURATED_PLACES.put("varanasi::tourist", spiritual);
        CURATED_PLACES.put("varanasi::historical", spiritual);
    }

    private static void initDelhiPlaces() {
        String city = "Delhi";
        List<PlannedPlace> hist = new ArrayList<>();
        hist.add(place("Red Fort (Lal Qila)", "Mughal Fortress & UNESCO Site", 28.6562, 77.2410, city, true));
        hist.add(place("Qutub Minar", "Historic 12th Century Victory Tower", 28.5245, 77.1855, city, true));
        hist.add(place("Humayun's Tomb", "Mughal Garden Tomb & UNESCO Site", 28.5933, 77.2507, city, true));
        hist.add(place("India Gate", "National War Memorial", 28.6129, 77.2295, city, true));
        hist.add(place("National Museum", "Premier Art & History Museum", 28.6118, 77.2193, city, true));
        CURATED_PLACES.put("delhi::historical", hist);

        List<PlannedPlace> spir = new ArrayList<>();
        spir.add(place("Lotus Temple (Bahá'í House of Worship)", "Architectural Marvel & Prayer Hall", 28.5535, 77.2588, city, true));
        spir.add(place("Akshardham Temple", "Spiritual & Cultural Campus", 28.6127, 77.2773, city, true));
        spir.add(place("Jama Masjid", "Historic 17th Century Mosque", 28.6507, 77.2334, city, true));
        spir.add(place("Gurudwara Bangla Sahib", "Sikh Pilgrimage & Sarovar", 28.6263, 77.2090, city, true));
        spir.add(place("Birla Mandir (Laxminarayan Temple)", "Historic Hindu Temple", 28.6327, 77.1988, city, true));
        CURATED_PLACES.put("delhi::spiritual", spir);
        CURATED_PLACES.put("delhi::tourist", hist);
    }

    private static void initJaipurPlaces() {
        String city = "Jaipur";
        List<PlannedPlace> hist = new ArrayList<>();
        hist.add(place("Amber Palace (Amer Fort)", "Hilltop Rajput Fortress", 26.9855, 75.8513, city, true));
        hist.add(place("Hawa Mahal (Palace of Winds)", "Iconic Pink Sandstone Facade", 26.9239, 75.8267, city, true));
        hist.add(place("City Palace, Jaipur", "Royal Palace & Heritage Museum", 26.9258, 75.8237, city, true));
        hist.add(place("Jantar Mantar", "UNESCO Astronomical Observatory", 26.9248, 75.8246, city, true));
        hist.add(place("Nahargarh Fort", "Scenic Ridge Fort", 26.9372, 75.8156, city, true));
        CURATED_PLACES.put("jaipur::historical", hist);
        CURATED_PLACES.put("jaipur::tourist", hist);

        List<PlannedPlace> spir = new ArrayList<>();
        spir.add(place("Govind Dev Ji Temple", "Sacred City Palace Mandir", 26.9280, 75.8240, city, true));
        spir.add(place("Birla Mandir (Laxmi Narayan)", "White Marble Temple", 26.8920, 75.8150, city, true));
        spir.add(place("Galtaji Temple (Monkey Temple)", "Ancient Hill Springs Complex", 26.9150, 75.8600, city, true));
        CURATED_PLACES.put("jaipur::spiritual", spir);
    }

    private static void initAgraPlaces() {
        String city = "Agra";
        List<PlannedPlace> hist = new ArrayList<>();
        hist.add(place("Taj Mahal", "UNESCO World Heritage Wonder", 27.1751, 78.0421, city, true));
        hist.add(place("Agra Fort", "Historic Mughal Imperial Residence", 27.1795, 78.0211, city, true));
        hist.add(place("Fatehpur Sikri", "Ancient Mughal Imperial City", 27.0945, 77.6679, city, true));
        hist.add(place("Itmad-ud-Daulah (Baby Taj)", "Mughal Mausoleum", 27.1929, 78.0310, city, true));
        hist.add(place("Mehtab Bagh", "Moonlight Garden across Taj", 27.1799, 78.0420, city, true));
        CURATED_PLACES.put("agra::historical", hist);
        CURATED_PLACES.put("agra::tourist", hist);
        CURATED_PLACES.put("agra::romantic", hist);
    }

    private static void initGoaPlaces() {
        String city = "Goa";
        List<PlannedPlace> nature = new ArrayList<>();
        nature.add(place("Calangute Beach", "Queen of Beaches", 15.5439, 73.7554, city, true));
        nature.add(place("Baga Beach", "Scenic Beach & Watersports", 15.5553, 73.7517, city, true));
        nature.add(place("Dudhsagar Falls", "Majestic Four-Tiered Waterfall", 15.3144, 74.3143, city, true));
        nature.add(place("Palolem Beach", "Serene Crescent Beach", 15.0100, 74.0232, city, true));
        nature.add(place("Anjuna Beach & Flea Market", "Coastal Cliffs & Market", 15.5833, 73.7431, city, true));
        CURATED_PLACES.put("goa::nature", nature);
        CURATED_PLACES.put("goa::relaxation", nature);
        CURATED_PLACES.put("goa::romantic", nature);
        CURATED_PLACES.put("goa::tourist", nature);

        List<PlannedPlace> spir = new ArrayList<>();
        spir.add(place("Basilica of Bom Jesus", "UNESCO Historic Baroque Church", 15.5009, 73.9116, city, true));
        spir.add(place("Se Cathedral", "16th Century Historic Cathedral", 15.5034, 73.9125, city, true));
        spir.add(place("Shanta Durga Temple", "Historic Hindu Pilgrimage Shrine", 15.3619, 73.9972, city, true));
        spir.add(place("Mangueshi Temple", "Famous Goan Shiva Mandir", 15.4419, 73.9686, city, true));
        CURATED_PLACES.put("goa::spiritual", spir);
        CURATED_PLACES.put("goa::historical", spir);
    }
}
