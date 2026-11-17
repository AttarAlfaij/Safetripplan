import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { apiFetch, getErrorMessage, readJson } from "../api/client.js";
import { useI18n } from "../i18n/I18nContext.jsx";

const MOODS = [
  { value: "Tourist", label: "🗺 Tourist" },
  { value: "Adventure", label: "🏔 Adventure" },
  { value: "Historical", label: "🏛 Historical" },
  { value: "Romantic", label: "💕 Romantic" },
  { value: "Nature", label: "🌿 Nature" },
  { value: "Relaxation", label: "☀ Relaxation" },
  { value: "Spiritual", label: "🕌 Spiritual" },
  { value: "Family Friendly", label: "👨‍👩‍👧 Family Friendly" },
];

const DEFAULT_FORM = { city: "", mood: "", days: "3" };

function getUserId() {
  return (
    localStorage.getItem("userId") ||
    localStorage.getItem("userEmail") ||
    "guest"
  );
}

function getStorageKeys() {
  const userId = getUserId();
  return {
    form: `planTripForm_${userId}`,
    data: `planTripData_${userId}`,
  };
}

function readStoredForm(key) {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== "object") return null;
    return {
      city: parsed.city || "",
      mood: parsed.mood || "",
      days: parsed.days !== undefined ? String(parsed.days) : "3",
    };
  } catch {
    return null;
  }
}

function readStoredData(key) {
  try {
    const raw = localStorage.getItem(key);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function dayNumber(key) {
  return parseInt(String(key).replace(/\D/g, ""), 10) || 0;
}

function getDragAfterElement(elements, y) {
  return elements.reduce(
    (closest, child) => {
      const box = child.getBoundingClientRect();
      const offset = y - box.top - box.height / 2;
      if (offset < 0 && offset > closest.offset) {
        return { offset, element: child };
      }
      return closest;
    },
    { offset: Number.NEGATIVE_INFINITY, element: null }
  ).element;
}


function getValidCoords(place) {
  if (!place) return null;
  const lat = parseFloat(place.lat);
  const lon = parseFloat(place.lon);
  if (!Number.isFinite(lat) || !Number.isFinite(lon)) return null;
  return { lat, lon };
}

function buildGoogleMapsUrl(place, city) {
  const name = (place && place.name) || "";
  const coords = getValidCoords(place);
  if (coords) {
    const q = `${name},${coords.lat},${coords.lon}`;
    return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`;
  }
  const q = [name, city].filter(Boolean).join(" ");
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(q)}`;
}

function buildOpenStreetMapUrl(place) {
  const coords = getValidCoords(place);
  if (!coords) return "";
  return `https://www.openstreetmap.org/?mlat=${coords.lat}&mlon=${coords.lon}#map=16/${coords.lat}/${coords.lon}`;
}

function buildPhotosUrl(place, city) {
  const q = [(place && place.name) || "", city].filter(Boolean).join(" ");
  return `https://www.google.com/search?tbm=isch&q=${encodeURIComponent(q)}`;
}
function Placeholder() {
  return (
    <article
      className="place-card placeholder"
      style={{
        border: "2px dashed var(--primary)",
        background: "rgba(224, 242, 254, 0.5)",
        opacity: 0.6,
      }}
    />
  );
}

export default function PlanTrip() {
  const { t } = useI18n();
  const storageKeys = getStorageKeys();

  const [form, setForm] = useState(
    () => readStoredForm(storageKeys.form) || DEFAULT_FORM
  );
  const [result, setResult] = useState(() =>
    readStoredData(storageKeys.data)
  );
  const [status, setStatus] = useState(() => {
    const restored = readStoredData(storageKeys.data);
    return restored
      ? { message: t("plan_generated_success"), type: "success" }
      : { message: "", type: "" };
  });
  const [loading, setLoading] = useState(false);
  const [myPlaces, setMyPlaces] = useState([]);
  const [toast, setToast] = useState("");
  const [drag, setDrag] = useState(null);

  const toastTimer = useRef(null);
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markersLayerRef = useRef(null);
  const mapTimerRef = useRef(null);

  useEffect(() => {
    document.title = "Plan Trip | Safe Trip Planner";
  }, []);

  useEffect(() => {
    localStorage.setItem(storageKeys.form, JSON.stringify(form));
  }, [form, storageKeys.form]);

  useEffect(() => {
    if (result) {
      localStorage.setItem(storageKeys.data, JSON.stringify(result));
    }
  }, [result, storageKeys.data]);

  const loadMyPlaces = useCallback(async () => {
    try {
      const response = await apiFetch("/my-places");
      if (!response.ok) {
        setMyPlaces([]);
        return;
      }
      const data = await readJson(response);
      setMyPlaces(Array.isArray(data) ? data : []);
    } catch {
      setMyPlaces([]);
    }
  }, []);

  useEffect(() => {
    loadMyPlaces();
  }, [loadMyPlaces]);

  useEffect(() => {
    return () => {
      if (toastTimer.current) {
        clearTimeout(toastTimer.current);
      }
    };
  }, []);

  const showToast = useCallback((placeName) => {
    if (toastTimer.current) {
      clearTimeout(toastTimer.current);
    }
    setToast(`✓ ${placeName || "Place"} added to My Trip!`);
    toastTimer.current = setTimeout(() => {
      setToast("");
    }, 2500);
  }, []);

  const itinerary = result && result.itinerary ? result.itinerary : null;

  const dayKeys = useMemo(() => {
    if (!itinerary) return [];
    return Object.keys(itinerary).sort((a, b) => dayNumber(a) - dayNumber(b));
  }, [itinerary]);

  const validPlaces = useMemo(() => {
    if (!itinerary) return [];
    const places = [];
    Object.values(itinerary).forEach((dayPlaces) => {
      (dayPlaces || []).forEach((place) => {
        const lat = parseFloat(place && place.lat);
        const lon = parseFloat(place && place.lon);
        if (!Number.isNaN(lat) && !Number.isNaN(lon)) {
          places.push(place);
        }
      });
    });
    return places;
  }, [itinerary]);

  const hasValidPlaces = validPlaces.length > 0;

  const destroyMap = useCallback(() => {
    if (mapTimerRef.current) {
      clearTimeout(mapTimerRef.current);
      mapTimerRef.current = null;
    }
    if (mapInstanceRef.current) {
      mapInstanceRef.current.remove();
      mapInstanceRef.current = null;
    }
    markersLayerRef.current = null;
  }, []);

  useEffect(() => {
    return () => {
      destroyMap();
    };
  }, [destroyMap]);

  useEffect(() => {
    if (!hasValidPlaces) {
      destroyMap();
      return undefined;
    }

    const container = mapRef.current;
    if (!container) {
      return undefined;
    }

    let map = mapInstanceRef.current;
    if (!map) {
      map = L.map(container);
      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors",
      }).addTo(map);
      markersLayerRef.current = L.layerGroup().addTo(map);
      mapInstanceRef.current = map;
    }

    const layer = markersLayerRef.current;
    layer.clearLayers();

    const bounds = [];
    validPlaces.forEach((place) => {
      const lat = parseFloat(place.lat);
      const lon = parseFloat(place.lon);
      L.marker([lat, lon])
        .addTo(layer)
        .bindPopup(place.name || "Place");
      bounds.push([lat, lon]);
    });

    mapTimerRef.current = setTimeout(() => {
      map.invalidateSize();
      if (bounds.length > 0) {
        map.fitBounds(bounds, { padding: [30, 30] });
      }
    }, 400);

    return () => {
      if (mapTimerRef.current) {
        clearTimeout(mapTimerRef.current);
        mapTimerRef.current = null;
      }
    };
  }, [hasValidPlaces, validPlaces, destroyMap]);

  const clearResults = () => {
    setResult(null);
  };

  const showError = (message) => {
    setStatus({
      message: message || t("error_generate_plan"),
      type: "error",
    });
  };

  const showSuccess = (message) => {
    setStatus({ message: message || "", type: "success" });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const city = form.city.trim();
    const mood = form.mood;
    const days = parseInt(form.days, 10);

    if (!city || !mood || !days || days < 1) {
      showError(t("error_fill_fields"));
      return;
    }

    setLoading(true);
    setStatus({ message: "", type: "" });
    clearResults();

    try {
      const response = await apiFetch("/plan-trip", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ city, mood, days }),
      });

      if (!response.ok) {
        const message = await getErrorMessage(
          response,
          t("error_generate_plan")
        );
        throw new Error(message);
      }

      const data = await readJson(response);
      if (!data || !data.itinerary) {
        showError(t("error_no_data"));
        return;
      }

      setResult(data);
      showSuccess(t("plan_generated_success"));
      loadMyPlaces();
    } catch (error) {
      showError(error && error.message ? error.message : t("error_generate_plan"));
    } finally {
      setLoading(false);
    }
  };

  const handleTogglePlace = async (place, dayKey, orderIndex) => {
    const existing = myPlaces.find((saved) => saved.name === place.name);

    if (existing) {
      const dbId = existing.id;
      if (!dbId) return;

      setMyPlaces((current) =>
        current.filter((saved) => saved.name !== place.name)
      );

      try {
        await apiFetch(`/my-places/${dbId}`, { method: "DELETE" });
      } catch (error) {
        console.error("[Remove] API error:", error);
        setMyPlaces((current) => {
          if (current.some((saved) => saved.name === place.name)) {
            return current;
          }
          return [...current, existing];
        });
      }
      return;
    }

    const currentTripName = (result && result.city) || form.city || "";
    const optimistic = {
      id: null,
      name: place.name || "",
      category: place.category || "",
      osmLink: place.osmLink || "",
      day: dayKey || "",
      tripName: currentTripName,
      orderIndex,
    };

    setMyPlaces((current) => [
      ...current.filter((saved) => saved.name !== place.name),
      optimistic,
    ]);
    showToast(place.name || "Place");

    try {
      const response = await apiFetch("/my-places", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: place.name || "Unnamed place",
          category: place.category || "",
          lat: place.lat || 0,
          lon: place.lon || 0,
          osmLink: place.osmLink || "",
          day: dayKey || "",
          tripName: currentTripName,
          orderIndex,
        }),
      });

      if (!response.ok) {
        throw new Error("Add failed");
      }

      const saved = await readJson(response);
      setMyPlaces((current) => {
        const without = current.filter(
          (entry) => entry.name !== place.name
        );
        if (without.some((entry) => entry.name === place.name)) {
          return without;
        }
        return [
          ...without,
          {
            id: saved.id,
            name: saved.name,
            category: saved.category,
            osmLink: saved.osmLink,
            day: saved.day,
            tripName: saved.tripName,
            orderIndex: saved.orderIndex,
          },
        ];
      });
    } catch (error) {
      console.error("[Add] API error:", error);
      setMyPlaces((current) =>
        current.filter((saved) => saved.name !== place.name)
      );
    }
  };

  const handleDragStart = (event, place, dayKey, index) => {
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", place.name || "");
    setTimeout(() => {
      setDrag({
        sourceDay: dayKey,
        sourceIndex: index,
        targetDay: dayKey,
        targetIndex: index,
      });
    }, 0);
  };

  const handleDragOver = (event, dayKey) => {
    event.preventDefault();
    if (!drag) return;

    event.dataTransfer.dropEffect = "move";

    const grid = event.currentTarget;
    const cards = Array.from(
      grid.querySelectorAll(".place-card:not(.placeholder):not(.dragging)")
    );
    const afterElement = getDragAfterElement(cards, event.clientY);
    const index = afterElement ? cards.indexOf(afterElement) : cards.length;

    setDrag((current) =>
      current
        ? { ...current, targetDay: dayKey, targetIndex: index }
        : current
    );
  };

  const handleDrop = (event, dayKey) => {
    event.preventDefault();
    if (!drag) return;

    const targetDay = dayKey;
    const targetIndex =
      drag.targetDay === dayKey ? drag.targetIndex : 0;

    setResult((current) => {
      if (!current || !current.itinerary) return current;

      const nextItinerary = {};
      Object.keys(current.itinerary).forEach((key) => {
        nextItinerary[key] = [...(current.itinerary[key] || [])];
      });

      const source = nextItinerary[drag.sourceDay];
      if (
        !source ||
        drag.sourceIndex < 0 ||
        drag.sourceIndex >= source.length
      ) {
        return current;
      }

      const [moved] = source.splice(drag.sourceIndex, 1);
      const target = nextItinerary[targetDay] || [];
      target.splice(targetIndex, 0, moved);
      nextItinerary[targetDay] = target;

      return { ...current, itinerary: nextItinerary };
    });

    setDrag(null);
  };

  const handleDragEnd = () => {
    setDrag(null);
  };

  const handleDragEnter = (event) => {
    event.preventDefault();
  };

  const city = (result && result.city) || form.city || "";
  const days = (result && result.days) || 0;

  return (
    <main className="plan-trip-page">
      <div className="page-shell">
        <header className="page-header">
          <div className="brand">
            <div className="brand-mark">ST</div>
            <div>
              <div className="brand-text-main">Safe Trip Planner</div>
              <span className="brand-sub">{t("brand_sub")}</span>
            </div>
          </div>
          <Link to="/" className="back-link">
            <span>←</span>
            <span>{t("back_to_home")}</span>
          </Link>
        </header>

        <div className="layout">
          <section className="card" aria-labelledby="plan-form-title">
            <h1 id="plan-form-title" className="card-title">
              {t("plan_form_title")}
            </h1>
            <p className="card-subtitle">{t("plan_form_subtitle")}</p>

            <form className="form-grid" onSubmit={handleSubmit} noValidate>
              <div className="field">
                <label htmlFor="city">{t("city_label")}</label>
                <input
                  id="city"
                  name="city"
                  type="text"
                  placeholder={t("city_placeholder")}
                  value={form.city}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      city: event.target.value,
                    }))
                  }
                  required
                />
                <span className="hint">{t("city_hint")}</span>
              </div>

              <div className="field">
                <label htmlFor="mood">{t("mood_label")}</label>
                <select
                  id="mood"
                  name="mood"
                  value={form.mood}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      mood: event.target.value,
                    }))
                  }
                  required
                >
                  <option value="">{t("mood_select")}</option>
                  {MOODS.map((mood) => (
                    <option key={mood.value} value={mood.value}>
                      {mood.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label htmlFor="days">{t("days_label")}</label>
                <input
                  id="days"
                  name="days"
                  type="number"
                  min="1"
                  value={form.days}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      days: event.target.value,
                    }))
                  }
                  required
                />
              </div>

              <div className="actions">
                <button type="submit" className="btn-primary">
                  <span />
                  {t("generate_plan")}
                </button>
                <div
                  className={loading ? "spinner visible" : "spinner"}
                  aria-hidden="true"
                >
                  <div className="spinner-dot" />
                  <span>{t("searching_places")}</span>
                </div>
              </div>

              <div className={`status-message ${status.type}`}>
                {status.message}
              </div>
            </form>
          </section>

          <section className="card" aria-live="polite">
            {itinerary ? (
              <div className="results-wrapper visible fade-in">
                <div className="results-header">
                  <div className="results-title">
                    {city
                      ? `${t("plan_for")} ${city}`
                      : t("trip_plan")}
                  </div>
                  <div className="results-meta">
                    {days
                      ? `${days} day${days > 1 ? "s" : ""} · ${t(
                          "generated_by"
                        )}`
                      : ""}
                  </div>
                </div>

                <div className="day-list">
                  {dayKeys.map((dayKey, index) => {
                    const places = itinerary[dayKey] || [];
                    const cards = places.map((place, placeIndex) => {
                      const isDragging =
                        drag &&
                        drag.sourceDay === dayKey &&
                        drag.sourceIndex === placeIndex;
                      const existing = myPlaces.find(
                        (saved) => saved.name === place.name
                      );

                      return (
                        <article
                          key={`${dayKey}-${place.name}-${placeIndex}`}
                          className={
                            isDragging
                              ? "place-card dragging"
                              : "place-card"
                          }
                          draggable
                          onDragStart={(event) =>
                            handleDragStart(
                              event,
                              place,
                              dayKey,
                              placeIndex
                            )
                          }
                          onDragEnd={handleDragEnd}
                        >
                          <div className="place-name">
                            {place.name || "Unnamed place"}
                          </div>
                          <div className="place-category">
                            {place.category || ""}
                          </div>
                          <div className="place-links">
                            <a
                              className="place-link-item"
                              href={buildGoogleMapsUrl(place, city)}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              <span aria-hidden="true"></span> {t("view_on_google_maps")}
                            </a>
                            {getValidCoords(place) ? (
                              <a
                                className="place-link-item"
                                href={buildOpenStreetMapUrl(place)}
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                <span aria-hidden="true"></span> {t("view_on_map")}
                              </a>
                            ) : null}
                            <a
                              className="place-link-item"
                              href={buildPhotosUrl(place, city)}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              <span aria-hidden="true"></span> {t("view_photos")}
                            </a>
                          </div>
                          <button
                            type="button"
                            className={
                              existing
                                ? "btn-add-trip added"
                                : "btn-add-trip"
                            }
                            title={
                              existing
                                ? t("remove_from_trip")
                                : t("add_to_trip")
                            }
                            onClick={() =>
                              handleTogglePlace(place, dayKey, placeIndex)
                            }
                          >
                            {existing ? "✕" : "+"}
                          </button>
                        </article>
                      );
                    });

                    if (drag && drag.targetDay === dayKey) {
                      cards.splice(
                        drag.targetIndex,
                        0,
                        <Placeholder key="placeholder" />
                      );
                    }

                    return (
                      <div
                        key={dayKey}
                        className="day"
                        style={{
                          animationDelay: `${0.05 + index * 0.07}s`,
                        }}
                      >
                        <div className="day-title">{dayKey}</div>
                        <div
                          className="places-grid"
                          data-day={dayKey}
                          data-trip={city}
                          onDragOver={(event) =>
                            handleDragOver(event, dayKey)
                          }
                          onDragEnter={handleDragEnter}
                          onDrop={(event) => handleDrop(event, dayKey)}
                        >
                          {places.length === 0 ? (
                            <div
                              className="empty-day-msg"
                              style={{
                                fontSize: "0.82rem",
                                color: "#6b7280",
                                padding: "1rem",
                                textAlign: "center",
                              }}
                            >
                              Drop places here.
                            </div>
                          ) : null}
                          {cards}
                        </div>
                      </div>
                    );
                  })}
                </div>

                {hasValidPlaces ? (
                  <div className="map-wrapper visible">
                    <div id="map" ref={mapRef} />
                  </div>
                ) : null}
              </div>
            ) : null}
          </section>
        </div>
      </div>

      {toast ? (
        <div id="trip-toast" className="show">
          {toast}
        </div>
      ) : null}
    </main>
  );
}
