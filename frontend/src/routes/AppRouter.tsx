import { createBrowserRouter, redirect } from "react-router-dom";
import Root from "./Root";
import ErrorGlobal from "../pages/ErrorGlobal";
import { authRoutes } from "./authRoutes";
import LandingPage from "../pages/landingPage";
import { getAuthToken, loader as tokenLoader } from "../hooks/useTokenDuration";
import { Home } from "../pages/Home";
import SessionLobby from "../components/SessionLobby/SessionLobby";
import ProtectedRoute from "./ProtectedRoute"; // <-- Importar el wrapper
import VotingBoard from "../components/VotingBoard/VotingBoard";

function redirectAuthenticated() {
  const token = getAuthToken();
  if (token && token !== "EXPIRED") {
    return redirect("/home");
  }
  return null;
}

export const router = createBrowserRouter([
  {
    id: "root",
    path: "/",
    element: <Root />,
    errorElement: <ErrorGlobal />,
    loader: tokenLoader,
    children: [
      {
        index: true,
        loader: redirectAuthenticated,
        element: <LandingPage />,
      },
      // 🛡️ Todas las rutas privadas agrupadas bajo el wrapper ProtectedRoute
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: "home",
            element: <Home />,
            children: [
              {
                index: true,
                element: <SessionLobby />,
              },
            ],
          },
          {
            path: "session/:code",
            element: <VotingBoard />,
          },
        ],
      },
      ...authRoutes,
    ],
  },
]);