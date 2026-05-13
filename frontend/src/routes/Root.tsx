
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

        const timer = setTimeout( () => {
            logout();
            submit(null, {action:"/auth/logout", method:"post" });
        }, timeRemaining);

        return () => clearTimeout(timer);

    } , [token, submit, logout]);

    return (
        <div className="min-h-dvh bg-background">
            <Outlet />
        </div>
    );
}