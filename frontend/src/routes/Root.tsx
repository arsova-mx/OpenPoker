
import { Outlet, useSubmit } from "react-router-dom";
import { useEffect } from "react";
import { getTokenDuration } from "@/hooks/useTokenDuration";
import useAuthStore from "@/store/authStore";

export default function Root() {
    const token = localStorage.getItem("token")
    const logout = useAuthStore( (state) => state.logout);
    const submit = useSubmit();

    useEffect( () => {

        if (!token) {
            return;
        }

        const timeRemaining = getTokenDuration();

        if(token ==='EXPIRED') {
            submit(null, {action:"/auth/logout", method:"post" });
            logout();
            console.log("Sesion cerrada: Token expirado")
        }
        
        setTimeout( () => {
            submit(null, {action:"/auth/logout", method:"post" });
            logout();
            console.log("Sesion cerrada: Tiempo agotado")
        }, timeRemaining);

    } , [token, submit]);

    return (
        <main className="place-content-start  p-10 min-h-dvh bg-cyan-100">
            <Outlet />
        </main>
    );
}