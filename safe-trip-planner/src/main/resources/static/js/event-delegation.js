// ===== EVENT DELEGATION FOR ADD/REMOVE PLACE BUTTONS =====
// Get user-specific storage key
const userEmail = localStorage.getItem("userEmail") || "guest";
const STORAGE_KEY = "myTripPlaces_" + userEmail;

// Override loadMyTrip function for my-trip.html if it exists
if (typeof window.loadMyTrip === 'undefined') {
    window.loadMyTrip = function() {
        const stored = localStorage.getItem(STORAGE_KEY);
        const places = stored ? JSON.parse(stored) : [];
        const container = document.getElementById("tripContainer");
        if (!container) return; // Not on my-trip page

        container.innerHTML = "";

        if (places.length === 0) {
            container.innerHTML = `
            <div class="empty-state">
                <div class="icon">🗺️</div>
                <h2>${window.t ? window.t("no_places_saved", "No places saved yet") : "No places saved yet"}</h2>
                <p>${window.t ? window.t("no_places_saved_desc", "Go to Plan Trip, generate your itinerary, and tap the + button on any place to add it here.") : "Go to Plan Trip, generate your itinerary, and tap the + button on any place to add it here."}</p>
                <a href="/plan-trip.html">${window.t ? window.t("plan_trip_cta", "Plan a Trip →") : "Plan a Trip →"}</a>
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
            <h3>${place.name || (window.t ? window.t("unnamed_place", "Unnamed place") : "Unnamed place")}</h3>
            <div class="category">${place.category || ""}</div>
            ${place.osmLink ? `<a class="map-link" href="${place.osmLink}" target="_blank" rel="noopener noreferrer">${window.t ? window.t("map_view", "View on OpenStreetMap") : "View on OpenStreetMap"}</a>` : ""}
            <button class="btn-remove" data-place-name="${(place.name || "").replace(/'/g, "\\'")}">${window.t ? window.t("remove_button", "✕ Remove") : "✕ Remove"}</button>
        `;

            grid.appendChild(card);
        });

        container.appendChild(grid);
    };
}

document.addEventListener("click", function(e) {
    console.log("Clicked element:", e.target);

    // Handle plan-trip.html add/remove buttons
    if (e.target.classList.contains("btn-add-trip")) {
        handleAddRemoveClick(e.target);
    }

    // Handle my-trip.html remove buttons
    if (e.target.classList.contains("btn-remove")) {
        handleMyTripRemoveClick(e.target);
    }
});

function handleAddRemoveClick(addBtn) {
    // Extract place data from DOM if data attributes are missing
    let placeName = addBtn.getAttribute("data-place-name") || "";
    let placeCategory = addBtn.getAttribute("data-place-category") || "";
    let placeLat = addBtn.getAttribute("data-place-lat") || "";
    let placeLon = addBtn.getAttribute("data-place-lon") || "";
    let placeOsmLink = addBtn.getAttribute("data-place-osm-link") || "";
    let dbId = addBtn.getAttribute("data-db-id") || "";

    // If no data attributes, extract from DOM
    if (!placeName) {
        const card = addBtn.closest(".place-card");
        if (card) {
            const nameEl = card.querySelector(".place-name");
            const categoryEl = card.querySelector(".place-category");
            const linkEl = card.querySelector(".place-link a");

            placeName = nameEl ? nameEl.textContent.trim() : "";
            placeCategory = categoryEl ? categoryEl.textContent.trim() : "";
            placeOsmLink = linkEl ? linkEl.href : "";

            console.log("Extracted from DOM:", { placeName, placeCategory, placeOsmLink });
        }
    }

    // Get translation function
    const t = window.t || function(key, fallback) { return fallback; };

    if (addBtn.classList.contains("added")) {
        // REMOVE
        const bucket = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
        const entry = bucket.find(function (p) { return p.id === actualDbId || p.name === placeName; });
        const actualDbId = entry && entry.id ? entry.id : dbId;

        if (!actualDbId) {
            // No DB id stored — just clean up localStorage
            console.warn("No DB id found for place, removing from localStorage only:", placeName);
            const updated = bucket.filter(function (p) { return !(p.id === actualDbId || p.name === placeName); });
            localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
            addBtn.textContent = "+";
            addBtn.classList.remove("added");
            addBtn.title = t("add_to_trip", "Add to My Trip");
            return;
        }

        // Disable button while request is in-flight
        addBtn.disabled = true;
        addBtn.style.opacity = "0.5";

        const token = localStorage.getItem("authToken") || "";
        fetch("/api/my-places/" + actualDbId, {
            method: "DELETE",
            headers: { "Authorization": "Bearer " + token }
        })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error("DELETE failed with status " + res.status);
                }
                // Success: update localStorage and UI
                const updated = bucket.filter(function (p) { return !(p.id === actualDbId || p.name === placeName); });
                localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
                addBtn.textContent = "+";
                addBtn.classList.remove("added");
                addBtn.title = t("add_to_trip", "Add to My Trip");
                console.log("[Remove] Deleted from DB and UI:", placeName, "id=" + actualDbId);
            })
            .catch(function (err) {
                console.error("[Remove] API error — UI not changed:", err);
            })
            .finally(function () {
                addBtn.disabled = false;
                addBtn.style.opacity = "";
            });

    } else {
        // ADD
        addBtn.disabled = true;
        addBtn.style.opacity = "0.5";

        const token = localStorage.getItem("authToken") || "";
        if (!token) {
            alert(t("login_to_save", "Please login to save places to your trip."));
            window.location.href = "/login.html";
            return;
        }
        fetch("/api/my-places", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify({
                name: placeName || t("unnamed_place", "Unnamed place"),
                category: placeCategory || "",
                lat: parseFloat(placeLat) || 0,
                lon: parseFloat(placeLon) || 0,
                osmLink: placeOsmLink || ""
            })
        })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error("POST failed with status " + res.status);
                }
                return res.json();
            })
            .then(function (savedPlace) {
                // savedPlace.id is the real DB primary key
                const bucket = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
                // Avoid duplicates (race condition guard)
                if (!bucket.some(function (p) { return p.id === savedPlace.id || p.name === placeName; })) {
                    bucket.push({
                        id: savedPlace.id,          // DB id for DELETE
                        name: savedPlace.name || placeName || t("unnamed_place", "Unnamed place"),
                        category: savedPlace.category || placeCategory || "",
                        osmLink: savedPlace.osmLink || placeOsmLink || ""
                    });
                    localStorage.setItem(STORAGE_KEY, JSON.stringify(bucket));
                }
                addBtn.textContent = "✕";
                addBtn.classList.add("added");
                addBtn.title = t("remove_from_trip", "Remove from My Trip");
                addBtn.setAttribute("data-db-id", savedPlace.id);

                // Show toast if function exists
                if (typeof showTripToast === 'function') {
                    showTripToast(placeName || t("unnamed_place", "Place"));
                }
                console.log("[Add] Saved to DB and UI:", placeName, "id=" + savedPlace.id);
            })
            .catch(function (err) {
                console.error("[Add] API error — UI not changed:", err);
            })
            .finally(function () {
                addBtn.disabled = false;
                addBtn.style.opacity = "";
            });
    }
}

function handleMyTripRemoveClick(btn) {
    // Get the place name from the closest card
    const card = btn.closest(".place-card");
    if (!card) return;

    const placeName = btn.getAttribute("data-place-name") ||
                      card.querySelector("h3")?.textContent || "";

    if (!placeName) {
        console.error("Could not determine place name for removal");
        return;
    }

    const stored = localStorage.getItem("myTripPlaces");
    const places = stored ? JSON.parse(stored) : [];
    const entry = places.find(function (p) { return p.name === placeName; });
    const dbId = entry && entry.id ? entry.id : null;

    if (!dbId) {
        // No DB id (old localStorage-only entry) — remove locally
        console.warn("[Remove] No DB id for:", placeName, "— removing from localStorage only.");
        const updated = places.filter(function (p) { return p.name !== placeName; });
        localStorage.setItem("myTripPlaces", JSON.stringify(updated));
        if (card) {
            card.style.transition = "opacity 0.25s ease, transform 0.25s ease";
            card.style.opacity = "0";
            card.style.transform = "scale(0.93)";
            setTimeout(function() {
                if (typeof loadMyTrip === 'function') {
                    loadMyTrip();
                }
            }, 260);
        } else {
            if (typeof loadMyTrip === 'function') {
                loadMyTrip();
            }
        }
        return;
    }

    // Disable the button while the API call is in-flight
    btn.disabled = true;
    btn.style.opacity = "0.5";

    fetch("/api/my-places/" + dbId, { method: "DELETE" })
        .then(function (res) {
            if (!res.ok) {
                throw new Error("DELETE failed with status " + res.status);
            }
            // Success: clean up localStorage, animate card out
            const updated = places.filter(function (p) { return p.name !== placeName; });
            localStorage.setItem("myTripPlaces", JSON.stringify(updated));
            console.log("[Remove] Deleted from DB and UI:", placeName, "id=" + dbId);
            if (card) {
                card.style.transition = "opacity 0.25s ease, transform 0.25s ease";
                card.style.opacity = "0";
                card.style.transform = "scale(0.93)";
                setTimeout(function() {
                    if (typeof loadMyTrip === 'function') {
                        loadMyTrip();
                    }
                }, 260);
            } else {
                if (typeof loadMyTrip === 'function') {
                    loadMyTrip();
                }
            }
        })
        .catch(function (err) {
            console.error("[Remove] API error — UI not changed:", err);
            btn.disabled = false;
            btn.style.opacity = "";
        });
}
