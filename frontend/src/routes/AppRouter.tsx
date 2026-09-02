import { createBrowserRouter, redirect } from "react-router-dom";
import Root from "./Root";
import ErrorGlobal from "../pages/ErrorGlobal";
import { authRoutes } from "./authRoutes";
import LandingPage from "../pages/landingPage";
import { getAuthToken, loader as tokenLoader } from "../hooks/useTokenDuration";
import { Home } from "../pages/Home";
import SessionLobby from "../components/SessionLobby/SessionLobby";

function requireAuth() {
  const token = getAuthToken();
  if (!token || token === "EXPIRED") {
    return redirect("/auth/login");
  }
  return null;
}

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
      {
        path: "home",
        loader: requireAuth,
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
        loader: requireAuth,
        element: (
          <div className="flex h-screen items-center justify-center text-xl font-bold">
            Placeholder Tablero de Votación (Sesión en desarrollo)
          </div>
        ),
      },
      ...authRoutes,
    ],
  },
]);