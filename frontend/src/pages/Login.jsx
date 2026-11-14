import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { oauthLoginUrl } from "../api/config.js";
import { useAuth } from "../auth/AuthContext.jsx";

export default function Login() {
  const navigate = useNavigate();
  const { user, loading } = useAuth();

  useEffect(() => {
    document.title = "Login | Safe Trip Planner";
  }, []);

  useEffect(() => {
    if (!loading && user) {
      navigate("/plan-trip", { replace: true });
    }
  }, [loading, navigate, user]);

  return (
    <main className="login-page">
      <div className="auth-card">
        <div className="panel active">
          <div className="panel-title">Welcome to Safe Trip 👋</div>
          <div className="panel-sub">Login with GitHub or Google to continue</div>

          <a href={oauthLoginUrl("github")} className="github-btn">
            Continue with GitHub
          </a>

          <a href={oauthLoginUrl("google")} className="google-btn">
            Continue with Google
          </a>

          <div className="msg">No password needed</div>
        </div>
      </div>
    </main>
  );
}
