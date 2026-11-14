import { useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";

export function useStartPlanning() {
  const navigate = useNavigate();
  const { user, loading, loginWithProvider } = useAuth();

  return useCallback(
    (event) => {
      if (loading) return;

      if (event) {
        event.preventDefault();
      }

      if (user) {
        navigate("/plan-trip");
      } else {
        loginWithProvider("github");
      }
    },
    [user, loading, loginWithProvider, navigate]
  );
}
