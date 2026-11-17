import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { oauthLoginUrl } from "../api/config.js";
import { useAuth } from "../auth/AuthContext.jsx";

export default function Login() {
  const navigate = useNavigate();
  const { user, loading: authLoading, login, register } = useAuth();

  const [activeTab, setActiveTab] = useState("login"); // 'login' or 'signup'
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    document.title =
      activeTab === "login"
        ? "Sign In | Safe Trip Planner"
        : "Create Account | Safe Trip Planner";
  }, [activeTab]);

  useEffect(() => {
    if (!authLoading && user) {
      navigate("/plan-trip", { replace: true });
    }
  }, [authLoading, navigate, user]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errorMessage) setErrorMessage("");
  };

  const handleTabSwitch = (tab) => {
    setActiveTab(tab);
    setErrorMessage("");
    setSuccessMessage("");
  };

  const validate = () => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email.trim()) {
      return "Please enter your email address.";
    }
    if (!emailRegex.test(formData.email.trim())) {
      return "Please enter a valid email address.";
    }
    if (!formData.password) {
      return "Please enter your password.";
    }
    if (formData.password.length < 6) {
      return "Password must be at least 6 characters long.";
    }
    if (activeTab === "signup") {
      if (formData.password !== formData.confirmPassword) {
        return "Passwords do not match.";
      }
    }
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage("");
    setSuccessMessage("");

    const clientValidationError = validate();
    if (clientValidationError) {
      setErrorMessage(clientValidationError);
      return;
    }

    setSubmitting(true);
    try {
      if (activeTab === "login") {
        await login(formData.email.trim(), formData.password);
        setSuccessMessage("Signed in successfully! Redirecting...");
        navigate("/plan-trip", { replace: true });
      } else {
        await register(
          formData.email.trim(),
          formData.password,
          formData.name.trim() || undefined
        );
        setSuccessMessage("Account created successfully! Redirecting...");
        navigate("/plan-trip", { replace: true });
      }
    } catch (err) {
      setErrorMessage(
        err.message || "An error occurred during authentication."
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="login-page">
      <div className="auth-card modern-auth-card">
        {/* Brand Header */}
        <div className="auth-header">
          <Link to="/" className="auth-logo-pill">
            <span className="auth-logo-dot" />
            Safe Trip Planner
          </Link>
          <h1 className="auth-main-title">
            {activeTab === "login" ? "Welcome back" : "Get started with Safe Trip"}
          </h1>
          <p className="auth-main-sub">
            {activeTab === "login"
              ? "Sign in to access your saved trips and personalized itineraries."
              : "Create an account to start planning and saving smart, safe trips."}
          </p>
        </div>

        {/* Tab Switcher */}
        <div className="auth-tabs" role="tablist">
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === "login"}
            className={`auth-tab-btn ${activeTab === "login" ? "active" : ""}`}
            onClick={() => handleTabSwitch("login")}
          >
            Sign In
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === "signup"}
            className={`auth-tab-btn ${activeTab === "signup" ? "active" : ""}`}
            onClick={() => handleTabSwitch("signup")}
          >
            Create Account
          </button>
        </div>

        {/* Feedback Messages */}
        {errorMessage && (
          <div className="auth-alert auth-alert-error" role="alert">
            <span className="alert-icon">⚠️</span>
            <span>{errorMessage}</span>
          </div>
        )}
        {successMessage && (
          <div className="auth-alert auth-alert-success" role="status">
            <span className="alert-icon">✅</span>
            <span>{successMessage}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          {activeTab === "signup" && (
            <div className="form-group">
              <label htmlFor="auth-name" className="form-label">
                Full Name
              </label>
              <input
                id="auth-name"
                name="name"
                type="text"
                autoComplete="name"
                placeholder="Jane Doe"
                className="form-input"
                value={formData.name}
                onChange={handleChange}
                disabled={submitting}
              />
            </div>
          )}

          <div className="form-group">
            <label htmlFor="auth-email" className="form-label">
              Email Address
            </label>
            <input
              id="auth-email"
              name="email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              className="form-input"
              value={formData.email}
              onChange={handleChange}
              disabled={submitting}
              required
            />
          </div>

          <div className="form-group">
            <div className="label-with-action">
              <label htmlFor="auth-password" className="form-label">
                Password
              </label>
              <button
                type="button"
                className="btn-toggle-pw"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
              >
                {showPassword ? "Hide" : "Show"}
              </button>
            </div>
            <input
              id="auth-password"
              name="password"
              type={showPassword ? "text" : "password"}
              autoComplete={activeTab === "login" ? "current-password" : "new-password"}
              placeholder="At least 6 characters"
              className="form-input"
              value={formData.password}
              onChange={handleChange}
              disabled={submitting}
              required
            />
          </div>

          {activeTab === "signup" && (
            <div className="form-group">
              <label htmlFor="auth-confirm-password" className="form-label">
                Confirm Password
              </label>
              <input
                id="auth-confirm-password"
                name="confirmPassword"
                type={showPassword ? "text" : "password"}
                autoComplete="new-password"
                placeholder="Re-enter your password"
                className="form-input"
                value={formData.confirmPassword}
                onChange={handleChange}
                disabled={submitting}
                required
              />
            </div>
          )}

          <button
            type="submit"
            className="auth-submit-btn"
            disabled={submitting}
          >
            {submitting ? (
              <span className="submitting-spinner">
                <span className="spinner-dot" /> Processing...
              </span>
            ) : activeTab === "login" ? (
              "Sign In with Email"
            ) : (
              "Create Account"
            )}
          </button>
        </form>

        {/* Divider */}
        <div className="auth-divider">
          <span>or continue with</span>
        </div>

        {/* OAuth Buttons (Preserved) */}
        <div className="oauth-button-stack">
          <a
            href={oauthLoginUrl("google")}
            className="oauth-btn google-oauth-btn"
          >
            <svg
              className="oauth-icon"
              viewBox="0 0 24 24"
              width="18"
              height="18"
              aria-hidden="true"
            >
              <path
                fill="#4285F4"
                d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.8-2.4 3.65v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.14z"
              />
              <path
                fill="#34A853"
                d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.33 24 12 24z"
              />
              <path
                fill="#FBBC05"
                d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.16 0 9.94 0 12s.45 3.84 1.25 5.42l4.03-3.15z"
              />
              <path
                fill="#EA4335"
                d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
              />
            </svg>
            <span>Continue with Google</span>
          </a>

          <a
            href={oauthLoginUrl("github")}
            className="oauth-btn github-oauth-btn"
          >
            <svg
              className="oauth-icon"
              viewBox="0 0 24 24"
              width="18"
              height="18"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
              />
            </svg>
            <span>Continue with GitHub</span>
          </a>
        </div>

        {/* Footer switch */}
        <div className="auth-card-footer">
          {activeTab === "login" ? (
            <p>
              Don't have an account?{" "}
              <button
                type="button"
                className="auth-link-button"
                onClick={() => handleTabSwitch("signup")}
              >
                Sign up
              </button>
            </p>
          ) : (
            <p>
              Already have an account?{" "}
              <button
                type="button"
                className="auth-link-button"
                onClick={() => handleTabSwitch("login")}
              >
                Sign in
              </button>
            </p>
          )}
          <div className="back-home-link-wrap">
            <Link to="/" className="back-home-link">
              ← Return to Home
            </Link>
          </div>
        </div>
      </div>
    </main>
  );
}
