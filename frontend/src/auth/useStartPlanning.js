import { useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";

export function useStartPlanning() {
  const navigate = useNavigate();
  const { user, loading } = useAuth();

  return useCallback(
    (event) => {
      if (loading) return;

      if (event) {
        event.preventDefault();
      }

      if (user) {
        navigate("/plan-trip");
      } else {
        navigate("/login");
      }
    },
    [user, loading, navigate]
  );
}
