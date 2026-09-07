import { Outlet } from "react-router-dom";
import Header from "../components/ui/header";

export function Home() {
  return (
    <div>
      <Header />
      <Outlet />
    </div>
  );
}
