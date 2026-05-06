import { authService } from "@/services/authService";
import { Outlet } from "react-router-dom";
import { useEffect } from "react";

export default function Root() {
    const token = localStorage.getItem("token")
    const tokenDuration = localStorage.getItem("tokenDuration");
    const logout = authService.logout;

    useEffect( () => {
        if (!token || !tokenDuration) {
            return;
        }
        const timeRemaining = (Date.now() + parseInt(tokenDuration)) - Date.now();
        console.log("Tiempo Restante: "+timeRemaining);

        if(timeRemaining <= 0) {
            logout();
        }
        
        const timer = setTimeout( () => {
            logout();
            console.log("Tiempo fuera (UseEffect)");
        }, timeRemaining);

        return () => clearTimeout(timer);

    } , [token, tokenDuration, logout]);

    return (
        <main className=" place-content-start  p-10 min-h-dvh bg-cyan-100">
            <Outlet />
        </main>
    );
}