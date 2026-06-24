// My Trip page functionality with user-specific localStorage
// Get user-specific storage key
const userEmail = localStorage.getItem("userEmail") || "guest";
const STORAGE_KEY = "myTripPlaces_" + userEmail;

function loadMyTrip() {
    const stored = localStorage.getItem(STORAGE_KEY);
    const places = stored ? JSON.parse(stored) : [];
    const container = document.getElementById("tripContainer");
    container.innerHTML = "";

    if (places.length === 0) {
        container.innerHTML = `
        <div class="empty-state">
            <div class="icon">🗺️</div>
            <h2>${t("no_places_saved", "No places saved yet")}</h2>
            <p>${t("no_places_saved_desc", "Go to Plan Trip, generate your itinerary, and tap the + button on any place to add it here.")}</p>
            <a href="/plan-trip.html">${t("plan_trip_cta", "Plan a Trip →")}</a>
        </div>
    `;
        return;
    }

    const grid = document.createElement("div");
    grid.className = "bucket-grid";

    places.forEach(function (place, idx) {
        const card = document.createElement("div");
        card.className = "place-card";
        card.style.animationDelay = (idx * 0.06) + "s";

        card.innerHTML = `
        <h3>${place.name || t("unnamed_place", "Unnamed place")}</h3>
        <div class="category">${place.category || ""}</div>
        ${place.osmLink ? `<a class="map-link" href="${place.osmLink}" target="_blank" rel="noopener noreferrer">${t("map_view", "View on OpenStreetMap")}</a>` : ""}
        <button class="btn-remove" data-place-name="${(place.name || "").replace(/'/g, "\\'")}">${t("remove_button", "✕ Remove")}</button>
    `;

        grid.appendChild(card);
    });

    container.appendChild(grid);
}

document.addEventListener("DOMContentLoaded", loadMyTrip);
