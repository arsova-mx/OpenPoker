
import { Outlet, useLoaderData, useSubmit } from "react-router-dom";
import { useEffect } from "react";
import { getTokenDuration } from "@/hooks/useTokenDuration";
import useAuthStore from "@/store/authStore";

export default function Root() {
    const token = useLoaderData();
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
        }
        
        setTimeout( () => {
            submit(null, {action:"/auth/logout", method:"post" });
            logout();
        }, timeRemaining);

    } , [token, submit]);

    return (
        <main className="place-content-start  p-10 min-h-dvh bg-cyan-100">
            <Outlet />
        </main>
    );
}