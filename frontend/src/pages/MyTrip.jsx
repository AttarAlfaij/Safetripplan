import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { jsPDF } from "jspdf";
import { apiFetch, readJson } from "../api/client.js";
import { useI18n } from "../i18n/I18nContext.jsx";

function getUserId() {
  return (
    localStorage.getItem("userId") ||
    localStorage.getItem("userEmail") ||
    "guest"
  );
}

function dayNumber(key) {
  return parseInt(String(key).replace(/\D/g, ""), 10) || 0;
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
function groupPlaces(places) {
  const grouped = {};

  places.forEach((place) => {
    const trip = place.tripName || "My Trip";
    const day = place.day || "Day 1";

    if (!grouped[trip]) grouped[trip] = {};
    if (!grouped[trip][day]) grouped[trip][day] = [];
    grouped[trip][day].push(place);
  });

  Object.keys(grouped).forEach((tripName) => {
    Object.keys(grouped[tripName]).forEach((day) => {
      grouped[tripName][day].sort(
        (a, b) =>
          (a.orderIndex ?? 9999) - (b.orderIndex ?? 9999)
      );
    });
  });

  return grouped;
}

export default function MyTrip() {
  const { t } = useI18n();
  const [places, setPlaces] = useState([]);

  useEffect(() => {
    document.title = "My Trip | Safe Trip Planner";
  }, []);

  const loadMyTrip = useCallback(async () => {
    try {
      const response = await apiFetch("/my-places");
      if (!response.ok) {
        setPlaces([]);
        return;
      }
      const data = await readJson(response);
      setPlaces(Array.isArray(data) ? data : []);
    } catch {
      setPlaces([]);
    }
  }, []);

  useEffect(() => {
    loadMyTrip();
  }, [loadMyTrip]);

  const grouped = useMemo(() => groupPlaces(places), [places]);

  const removeLocalStorageEntry = (dbId) => {
    try {
      const storageKey = `myTripPlaces_${getUserId()}`;
      const bucket = JSON.parse(localStorage.getItem(storageKey) || "[]");
      const updated = bucket.filter((place) => place.id !== dbId);
      localStorage.setItem(storageKey, JSON.stringify(updated));
    } catch {
      return;
    }
  };

  const removePlace = async (dbId) => {
    if (!dbId) {
      console.warn("[Remove] No DB id for this place.");
      return;
    }

    setPlaces((current) => current.filter((place) => place.id !== dbId));
    removeLocalStorageEntry(dbId);

    try {
      await apiFetch(`/my-places/${dbId}`, { method: "DELETE" });
    } catch (error) {
      console.error("[Remove] API error:", error);
      loadMyTrip();
    }
  };

  const removeAllPlaces = async () => {
    if (places.length === 0) return;

    if (!window.confirm("Are you sure you want to remove all saved places?")) {
      return;
    }

    setPlaces([]);
    localStorage.removeItem(`myTripPlaces_${getUserId()}`);

    try {
      const response = await apiFetch("/my-places", { method: "DELETE" });
      if (!response.ok) {
        throw new Error(`DELETE all failed: ${response.status}`);
      }
    } catch (error) {
      console.error("[Remove All] API error:", error);
      loadMyTrip();
    }
  };

  const downloadPDF = () => {
    if (places.length === 0) {
      window.alert("No places saved to export.");
      return;
    }

    const pdfGroups = groupPlaces(places);
    const doc = new jsPDF({ unit: "mm", format: "a4" });

    const margin = 18;
    const pageH = doc.internal.pageSize.getHeight();
    const pageW = doc.internal.pageSize.getWidth();
    let y = margin;

    const checkPage = (needed) => {
      if (y + needed > pageH - margin) {
        doc.addPage();
        y = margin;
      }
    };

    doc.setFont("helvetica", "bold");
    doc.setFontSize(18);
    doc.setTextColor(10, 25, 47);
    doc.text("Safe Trip Planner", margin, y);
    y += 10;

    Object.keys(pdfGroups).forEach((tripName) => {
      checkPage(18);

      doc.setFont("helvetica", "bold");
      doc.setFontSize(14);
      doc.setTextColor(0, 134, 160);
      doc.text(`Trip: ${tripName}`, margin, y);
      y += 3;

      doc.setDrawColor(0, 184, 216);
      doc.setLineWidth(0.6);
      doc.line(margin, y, pageW - margin, y);
      y += 6;

      const dayKeys = Object.keys(pdfGroups[tripName]).sort(
        (a, b) => dayNumber(a) - dayNumber(b)
      );

      dayKeys.forEach((dayKey) => {
        checkPage(14);

        doc.setFont("helvetica", "bold");
        doc.setFontSize(11);
        doc.setTextColor(15, 23, 42);
        doc.text(dayKey, margin, y);
        y += 6;

        pdfGroups[tripName][dayKey].forEach((place) => {
          checkPage(7);
          doc.setFont("helvetica", "normal");
          doc.setFontSize(10);
          doc.setTextColor(50, 50, 50);

          const label =
            `• ${place.name || "Unnamed place"}` +
            (place.category ? `  [${place.category}]` : "");
          const lines = doc.splitTextToSize(
            label,
            pageW - margin * 2 - 4
          );

          lines.forEach((line) => {
            checkPage(6);
            doc.text(line, margin + 4, y);
            y += 6;
          });
        });

        y += 3;
      });

      y += 8;
    });

    doc.save("safe-trip-planner.pdf");
  };

  const hasPlaces = places.length > 0;

  return (
    <main className="my-trip-page">
      <div className="page-shell">
        <div className="page-heading">
          <h1>{t("bucket_title")}</h1>
          <p>{t("bucket_subtitle")}</p>
          <button
            type="button"
            id="btn-download-pdf"
            className="btn-download-pdf"
            onClick={downloadPDF}
          >
            📄 Download PDF
          </button>
          <button
            type="button"
            id="btn-remove-all"
            className="btn-remove-all"
            onClick={removeAllPlaces}
          >
            🗑️ Remove All Places
          </button>
        </div>

        <div id="tripContainer">
          {!hasPlaces ? (
            <div className="empty-state">
              <div className="icon">🗺️</div>
              <h2>{t("no_places_saved")}</h2>
              <p>
                {t("no_places_desc", "Go to Plan Trip, generate your itinerary, and tap the + button on any place to add it here.")}
              </p>
              <Link to="/plan-trip">{t("plan_a_trip")}</Link>
            </div>
          ) : (
            Object.keys(grouped).map((tripName) => {
              const dayKeys = Object.keys(grouped[tripName]).sort(
                (a, b) => dayNumber(a) - dayNumber(b)
              );

              return (
                <div
                  key={tripName}
                  className="trip-section"
                  style={{ marginBottom: "2.5rem" }}
                >
                  <h2
                    style={{
                      fontSize: "1.5rem",
                      fontWeight: 700,
                      color: "var(--text-main)",
                      padding: "0.6rem 0 0.5rem",
                      marginBottom: "0.2rem",
                      borderBottom: "3px solid var(--primary)",
                    }}
                  >
                    {tripName}
                  </h2>

                  {dayKeys.map((dayKey, dayIdx) => (
                    <div
                      key={dayKey}
                      className="day"
                      style={{
                        animationDelay: `${0.05 + dayIdx * 0.07}s`,
                      }}
                    >
                      <div className="day-title">{dayKey}</div>
                      <div className="places-grid">
                        {grouped[tripName][dayKey].map((place, idx) => (
                          <article
                            key={place.id ?? `${place.name}-${idx}`}
                            className="place-card"
                            style={{
                              animationDelay: `${idx * 0.06}s`,
                            }}
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
                                href={buildGoogleMapsUrl(place, place.tripName || tripName)}
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
                                href={buildPhotosUrl(place, place.tripName || tripName)}
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                <span aria-hidden="true"></span> {t("view_photos")}
                              </a>
                            </div>
                            <button
                              type="button"
                              className="btn-remove"
                              onClick={() => removePlace(place.id)}
                            >
                              {t("remove")}
                            </button>
                          </article>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              );
            })
          )}
        </div>
      </div>
    </main>
  );
}
