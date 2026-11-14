import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";
import { useStartPlanning } from "../auth/useStartPlanning.js";
import { useI18n } from "../i18n/I18nContext.jsx";
import { SUPPORTED_LANGUAGES } from "../i18n/translations.js";
import { oauthLoginUrl } from "../api/config.js";

const LINKS = [
  {
    key: "home",
    href: "/",
    matches: ["/", "/home"],
  },
  {
    key: "about",
    href: "/about",
    matches: ["/about"],
  },
  {
    key: "plan_trip",
    href: "/plan-trip",
    matches: ["/plan-trip"],
    requiresAuth: true,
  },
  {
    key: "my_trip",
    href: "/my-trip",
    matches: ["/my-trip"],
  },
];

export default function Navbar() {
  const location = useLocation();
  const { user, loading, logout } = useAuth();
  const { lang, setLang, t } = useI18n();
  const startPlanning = useStartPlanning();

  const isActive = (link) =>
    link.matches.some((match) => location.pathname === match);

  return (
    <header className="stp-header">
      <div className="stp-header-inner">
        <Link to="/" className="stp-logo">
          <div className="stp-logo-mark">ST</div>
          <div className="stp-logo-text">
            <span className="stp-logo-main">Safe Trip Planner</span>
            <span className="stp-logo-sub">{t("logo_sub")}</span>
          </div>
        </Link>

        <nav className="stp-nav" aria-label="Main navigation">
          {LINKS.map((link) => (
            <Link
              key={link.key}
              to={link.href}
              className={isActive(link) ? "stp-active" : undefined}
              onClick={link.requiresAuth ? startPlanning : undefined}
            >
              {t(link.key)}
            </Link>
          ))}
        </nav>

        <div className="stp-nav-actions">
          <div className="stp-lang-switcher">
            <select
              id="lang-switcher"
              className="stp-lang-select"
              title="Select Language"
              value={lang}
              onChange={(event) => setLang(event.target.value)}
            >
              {SUPPORTED_LANGUAGES.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div className="stp-nav-auth">
            {!loading && user ? (
              <>
                <div className="stp-user-chip">
                  {user.avatarUrl ? (
                    <img
                      src={user.avatarUrl}
                      alt={user.name || user.username || ""}
                      className="stp-user-avatar"
                    />
                  ) : (
                    <span className="stp-user-avatar">
                      {(user.name || user.username || "?").charAt(0).toUpperCase()}
                    </span>
                  )}
                  <span>{user.name || user.username || ""}</span>
                </div>
                <button
                  type="button"
                  className="stp-btn-logout"
                  onClick={logout}
                >
                  {t("logout")}
                </button>
              </>
            ) : !loading ? (
              <div className="stp-auth-buttons">
                <a
                  href={oauthLoginUrl("github")}
                  className="stp-login-link"
                >
                  Login with GitHub
                </a>
                <a
                  href={oauthLoginUrl("google")}
                  className="stp-google-link"
                >
                  Continue with Google
                </a>
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </header>
  );
}
