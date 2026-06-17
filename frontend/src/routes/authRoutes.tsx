import Register from "../pages/Register";
import Auth from "../pages/Auth";
import { redirect } from "react-router-dom";
import { getAuthToken } from "../hooks/useTokenDuration";
import Login from "../pages/Login";

function redirectAuthenticated() {
  const token = getAuthToken();
  if (token && token !== "EXPIRED") {
    return redirect("/home");
  }
  return null;
}


export const authRoutes = [
  {
    path: "auth",
    element: <Auth />,
    loader: redirectAuthenticated,
    action: async () => redirect("/home"),
    children: [
      {
        path: "login",
        element: <Login />,
      },
      {
        path: "register",
        element: <Register />,
      }
    ],
  },
];