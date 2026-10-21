import { createBrowserRouter, redirect } from "react-router-dom";
import Root from "./Root";
import ErrorGlobal from "../pages/ErrorGlobal";
import { authRoutes } from "./authRoutes";
import LandingPage from "../pages/landingPage";
import { getAuthToken, loader as tokenLoader } from "../hooks/useTokenDuration";
import { Home } from "../pages/Home";
import SessionLobby from "../components/SessionLobby/SessionLobby";
import ProtectedRoute from "./ProtectedRoute";
import VotingBoard from "../components/VotingBoard/VotingBoard";
import NotFound from "../pages/NotFound";

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
      // Rutas privadas protegidas
      {
        element: <ProtectedRoute />,
        children: [
          // Subrutas principales envueltas en Home (Header + Outlet)
          {
            path: "home",
            element: <Home />,
            children: [
              {
                index: true,
                element: <SessionLobby />,
              },
              {
                path: "sessions",
                element: (
                  <div className="flex h-[60vh] items-center justify-center text-sm text-muted-foreground">
                    Próximamente: Histórico de sesiones (#40)
                  </div>
                ),
              },
            ],
          },
          // /session/:code envuelto en Home para heredar el Header
          {
            path: "session",
            element: <Home />,
            children: [
              {
                path: ":code",
                element: <VotingBoard />,
              },
            ],
          },
        ],
      },
      ...authRoutes,
      // Manejo de rutas desconocidas hacia NotFound
      {
        path: "*",
        element: <NotFound />,
      },
    ],
  },
]);