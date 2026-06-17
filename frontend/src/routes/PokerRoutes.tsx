import { redirect } from "react-router-dom";
import Dashboard from "../pages/Dashboard";


export const PokerRoutex = [
    {
    path: "/Dashboard",
    element: <Dashboard />,
    action: async () => redirect("/"),
  },

]