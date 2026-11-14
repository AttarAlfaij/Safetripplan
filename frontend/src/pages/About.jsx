import { useEffect } from "react";
import { useI18n } from "../i18n/I18nContext.jsx";

export default function About() {
  const { t } = useI18n();

  useEffect(() => {
    document.title = "About | Safe Trip Planner";
  }, []);

  return (
    <main className="about-page">
      <section className="about-hero" aria-labelledby="about-page-title">
        <div className="container">
          <div className="about-hero-tag">
            <span className="about-hero-dot" />
            <span>{t("about_hero_tag")}</span>
          </div>
          <h1 id="about-page-title">{t("about_hero_title")}</h1>
          <p>{t("about_hero_subtitle")}</p>
        </div>
      </section>

      <section className="platform-section" aria-labelledby="platform-title">
        <div className="container">
          <div className="platform-inner">
            <div>
              <p className="platform-kicker">{t("platform_kicker")}</p>
              <h2 id="platform-title" className="platform-title">
                {t("platform_title")}
              </h2>
              <p className="platform-body">{t("platform_body")}</p>

              <div className="platform-points">
                <div className="platform-point">
                  <div className="platform-point-icon">🧭</div>
                  <div>
                    <strong>{t("discover_confidently")}</strong>
                    <span>{t("discover_confidently_desc")}</span>
                  </div>
                </div>
                <div className="platform-point">
                  <div className="platform-point-icon">🗺️</div>
                  <div>
                    <strong>{t("plan_with_clarity")}</strong>
                    <span>{t("plan_with_clarity_desc")}</span>
                  </div>
                </div>
                <div className="platform-point">
                  <div className="platform-point-icon">🛡️</div>
                  <div>
                    <strong>{t("travel_safely")}</strong>
                    <span>{t("travel_safely_desc")}</span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <div className="platform-stat-card" aria-hidden="true">
                <div className="platform-stat-badge">
                  {t("live_trip_overview")}
                </div>

                <div className="platform-stat">
                  <span>{t("trips_organized")}</span>
                  <strong>10k+</strong>
                </div>
                <div className="platform-stat">
                  <span>{t("traveler_satisfaction")}</span>
                  <strong>4.9/5</strong>
                </div>
                <div className="platform-ribbon" />
              </div>
            </div>
          </div>
        </div>
      </section>

      <section id="contact" className="contact" aria-labelledby="contact-title">
        <div className="container">
          <div className="contact-inner">
            <div>
              <h2 id="contact-title" className="contact-main-title">
                {t("contact_title")}
              </h2>
              <p className="contact-text">{t("contact_text")}</p>
            </div>
            <div className="contact-links">
              <span>
                {t("email_label")}: alfaijattar@gmail.com
              </span>
              <span>{t("support_hours")}</span>
            </div>
          </div>

          <div className="footer-bottom">
            <span>{t("copyright")}</span>
            <div className="footer-meta">
              <span>{t("footer_meta")}</span>
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}
