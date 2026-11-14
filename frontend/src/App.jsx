import { useEffect } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import Navbar from "./components/Navbar.jsx";
import ProtectedRoute from "./components/ProtectedRoute.jsx";
import Home from "./pages/Home.jsx";
import About from "./pages/About.jsx";
import Login from "./pages/Login.jsx";
import PlanTrip from "./pages/PlanTrip.jsx";
import MyTrip from "./pages/MyTrip.jsx";
import EmptyPage from "./pages/EmptyPage.jsx";

const NO_NAVBAR_PATHS = new Set(["/login", "/dashboard", "/risk-map"]);

export default function App() {
  const location = useLocation();
  const showNavbar = !NO_NAVBAR_PATHS.has(location.pathname);

  useEffect(() => {
    document.body.classList.toggle("has-navbar", showNavbar);
    return () => document.body.classList.remove("has-navbar");
  }, [showNavbar]);

  return (
    <>
      {showNavbar && <Navbar />}
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/home" element={<Home />} />
        <Route path="/about" element={<About />} />
        <Route path="/login" element={<Login />} />
        <Route
          path="/plan-trip"
          element={
            <ProtectedRoute>
              <PlanTrip />
            </ProtectedRoute>
          }
        />
        <Route path="/my-trip" element={<MyTrip />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <EmptyPage />
            </ProtectedRoute>
          }
        />
        <Route path="/risk-map" element={<EmptyPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}
