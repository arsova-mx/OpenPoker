import { createBrowserRouter } from "react-router-dom";
import { redirect } from "react-router-dom";
import Root from "./Root";
import ErrorGlobal from "../pages/ErrorGlobal";
import { authRoutes } from "./authRoutes";
import LandingPage from "../pages/landingPage";
import { getAuthToken, loader as tokenLoader } from "../hooks/useTokenDuration";
import Dashboard from "../pages/Dashboard";
import { Home } from "../pages/Home";

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
        element: <LandingPage/>,
      },

      {
        path: "home",
        loader: requireAuth,
        element: <Home />,
        children: [
          {
            index: true,
            element: <Dashboard />,
          },
        ],
      },

      ...authRoutes,
      
    ],
  },
])