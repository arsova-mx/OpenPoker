
import { Outlet, useLoaderData, useSubmit } from "react-router-dom";
import { useEffect } from "react";
import { getTokenDuration } from "@/hooks/useTokenDuration";
import useAuthStore from "@/store/authStore";

export default function Root() {
    const token = useLoaderData();
    const isAuthenticated = useAuthStore( (state) => state.isAuthenticated)
    const logout = useAuthStore( (state) => state.logout);
    const submit = useSubmit();

    function handleLogout() {
        logout();
        submit(null, {action:"/auth/logout", method:"post" });
    }
 
    useEffect( () => {

<<<<<<< HEAD
        if (!token || !isAuthenticated) {
            handleLogout();
=======
        if (!token) {
>>>>>>> eb97fa4a88d104abe6a257c59949fa9a1459d359
            return;
        }

        const timeRemaining = getTokenDuration();

        if(timeRemaining < 0) {
            handleLogout();
            return;
        }

        const timer = setTimeout( () => {
            handleLogout();
        }, timeRemaining);

        return () => clearTimeout(timer);

    } , [token, submit, logout]);

    return (
        <div className="min-h-dvh bg-background">
            <Outlet />
        </div>
    );
}