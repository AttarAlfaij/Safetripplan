import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext.jsx";

export default function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();

  if (loading) {
    return <main className="page-loading" aria-busy="true" />;
  }

  if (!user) {
    return <Navigate to="/" replace />;
  }

  return children;
}
