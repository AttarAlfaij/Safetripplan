
import { useEffect } from "react";
import { Link } from "react-router-dom";
import { useStartPlanning } from "../auth/useStartPlanning.js";
import { useI18n } from "../i18n/I18nContext.jsx";

export default function Home() {
  const { t } = useI18n();
  const startPlanning = useStartPlanning();

  useEffect(() => {
    document.title = "Safe Trip Planner | Explore the World Safely";
  }, []);

  const title = t("home_title");
  const lastSpace = title.lastIndexOf(" ");
  const firstTitle = lastSpace === -1 ? title : title.slice(0, lastSpace);
  const lastTitle = lastSpace === -1 ? "" : title.slice(lastSpace + 1);

  const styles = {
    hero: {
      position: "relative",
      minHeight: "calc(100vh - 60px)",
      width: "100%",
      display: "flex",
      alignItems: "center",
      overflow: "hidden",
      backgroundColor: "#FFFFFF",
      isolation: "isolate",
    },

    inner: {
      width: "min(1084px, calc(100% - 48px))",
      margin: "0 auto",
      padding: "80px 0",
    },

    content: {
      maxWidth: "650px",
    },

    tag: {
      display: "inline-flex",
      alignItems: "center",
      gap: "10px",
      padding: "9px 16px",
      marginBottom: "20px",
      border: "1px solid rgba(0,191,166,0.28)",
      borderRadius: "50px",
      background: "rgba(0,191,166,0.10)",
      color: "#00796b",
      fontSize: "11px",
      fontWeight: 600,
      letterSpacing: "2px",
      textTransform: "uppercase",
    },

    dot: {
      width: "9px",
      height: "9px",
      flexShrink: 0,
      borderRadius: "50%",
      background: "#00BFA6",
      boxShadow: "0 0 12px rgba(0,191,166,0.55)",
    },

    title: {
      margin: "0 0 20px",
      color: "#123047",
      fontSize: "clamp(36px, 5vw, 48px)",
      fontWeight: 800,
      lineHeight: 1.12,
      letterSpacing: "-1.5px",
    },

    accent: {
      display: "block",
      color: "#00BFA6",
    },

    subtitle: {
      maxWidth: "490px",
      margin: "0 0 30px",
      color: "#526575",
      fontSize: "17px",
      lineHeight: 1.65,
    },

    actions: {
      display: "flex",
      alignItems: "center",
      marginBottom: "23px",
    },

    button: {
      minHeight: "58px",
      padding: "0 30px",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      gap: "13px",
      border: "none",
      borderRadius: "50px",
      background: "linear-gradient(110deg,#00d6a9,#00bcca)",
      color: "#002d42",
      textDecoration: "none",
      fontSize: "13px",
      fontWeight: 800,
      letterSpacing: "1.3px",
      boxShadow: "0 12px 35px rgba(0,207,190,0.30)",
      transition: "transform 0.25s ease, box-shadow 0.25s ease",
    },

    meta: {
      display: "flex",
      flexWrap: "wrap",
      gap: "25px",
      color: "#526575",
      fontSize: "13px",
    },

    metaItem: {
      display: "inline-flex",
      alignItems: "center",
      gap: "8px",
    },

    metaDot: {
      width: "7px",
      height: "7px",
      borderRadius: "50%",
      background: "#00BFA6",
    },
  };

  return (
    <main id="home" className="home-page">

      {/* HERO SECTION */}
      <section
        className="hero"
        aria-labelledby="hero-title"
        style={styles.hero}
      >
        <div style={styles.inner}>
          <div style={styles.content}>

            {/* Trust Badge */}
            <div style={styles.tag}>
              <span style={styles.dot} />
              {t("hero_tag")}
            </div>

            {/* Main Heading */}
            <h1 id="hero-title" style={styles.title}>
              {firstTitle}
              {lastTitle && (
                <span style={styles.accent}>
                  {lastTitle}
                </span>
              )}
            </h1>

            {/* Description */}
            <p style={styles.subtitle}>
              {t("hero_subtitle")}
            </p>

            {/* Start Planning Button */}
            <div style={styles.actions}>
              <Link
                to="/plan-trip"
                onClick={startPlanning}
                style={styles.button}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = "translateY(-3px)";
                  e.currentTarget.style.boxShadow =
                    "0 16px 40px rgba(0,207,190,0.42)";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = "translateY(0)";
                  e.currentTarget.style.boxShadow =
                    "0 12px 35px rgba(0,207,190,0.30)";
                }}
              >
                <span style={{ fontSize: "19px" }}>➜</span>
                {t("start_planning")}
              </Link>
            </div>

            {/* Hero Features */}
            <div style={styles.meta}>
              <span style={styles.metaItem}>
                <span style={styles.metaDot} />
                {t("hero_meta_1")}
              </span>

              <span style={styles.metaItem}>
                <span style={styles.metaDot} />
                {t("hero_meta_2")}
              </span>
            </div>

          </div>
        </div>
      </section>

      {/* ABOUT SECTION */}
      <section
        id="destinations"
        className="about"
        aria-labelledby="about-title"
      >
        <div className="container">
          <div className="about-inner">

            <div>
              <div className="about-highlight">
                {t("why_stp")}
              </div>

              <header className="section-heading">
                <p className="section-kicker">
                  {t("about_kicker")}
                </p>

                <h2 id="about-title" className="section-title">
                  {t("about_title")}
                </h2>

                <p className="section-subtitle">
                  {t("about_subtitle")}
                </p>
              </header>

              <p className="about-text">
                {t("about_text")}
              </p>

              <div className="about-points">
                <div className="about-point">
                  <span>{t("discover_confidently")}</span>
                  <span>{t("discover_confidently_desc")}</span>
                </div>

                <div className="about-point">
                  <span>{t("plan_with_clarity")}</span>
                  <span>{t("plan_with_clarity_desc")}</span>
                </div>
              </div>
            </div>

            <div className="about-visual" aria-hidden="true">
              <div className="about-card">

{/*                 <div className="about-badge"> */}
{/*                   {t("live_trip_overview")} */}
{/*                 </div> */}

{/*                 <div className="about-stat"> */}
{/*                   <span>{t("avg_planning_time")}</span> */}
{/*                   <strong>12 min</strong> */}
{/*                 </div> */}

{/*                 <div className="about-stat"> */}
{/*                   <span>{t("trips_organized")}</span> */}
{/*                   <strong>10k+</strong> */}
{/*                 </div> */}

{/*                 <div className="about-stat"> */}
{/*                   <span>{t("traveler_satisfaction")}</span> */}
{/*                   <strong>4.9/5</strong> */}
{/*                 </div> */}

                <div className="about-ribbon" /><span> Travel Smartly</span>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* FEATURES SECTION */}
      <section
        id="plan"
        className="features"
        aria-labelledby="features-title"
      >
        <div className="container">

          <header className="features-header">
            <p className="section-kicker">
              {t("features_kicker")}
            </p>

            <h2 id="features-title" className="section-title">
              {t("features_title")}
            </h2>

            <p
              className="section-subtitle"
              style={{
                maxWidth: "32rem",
                marginInline: "auto",
              }}
            >
              {t("features_subtitle")}
            </p>
          </header>

          <div className="features-grid" aria-label="Key features">

            <article className="feature-card">
              <div className="feature-icon">🧭</div>
              <h3 className="feature-title">
                {t("discover_destinations")}
              </h3>
              <p className="feature-text">
                {t("discover_destinations_desc")}
              </p>
            </article>

            <article className="feature-card">
              <div className="feature-icon">🗺️</div>
              <h3 className="feature-title">
                {t("smart_trip_planning")}
              </h3>
              <p className="feature-text">
                {t("smart_trip_planning_desc")}
              </p>
            </article>

            <article className="feature-card">
              <div className="feature-icon">🛡️</div>
              <h3 className="feature-title">
                {t("safe_secure_travel")}
              </h3>
              <p className="feature-text">
                {t("safe_secure_travel_desc")}
              </p>
            </article>

          </div>
        </div>
      </section>

      {/* FINAL CTA SECTION */}
      <section className="plan" aria-labelledby="plan-cta-title">
        <div className="container plan-inner">

          <div>
            <h2 id="plan-cta-title" className="plan-title">
              {t("cta_title")}
            </h2>

            <p className="plan-subtitle">
              {t("cta_subtitle")}
            </p>
          </div>

          <div className="plan-actions">
            <Link
              to="/plan-trip"
              className="btn-primary"
              onClick={startPlanning}
            >
              {t("start_planning")}
            </Link>

            <div className="plan-pill">
              {t("no_downloads")}
            </div>
          </div>

        </div>
      </section>

    </main>
  );
}
