
import { Outlet, useLoaderData, useNavigate, useSubmit } from "react-router-dom";
import { useCallback, useEffect } from "react";
import { getTokenDuration } from "@/hooks/useTokenDuration";
import useAuthStore from "@/store/authStore";
import { toast } from "sonner";

export default function Root() {
    const token = useLoaderData();
    const isAuthenticated = useAuthStore( (state) => state.isAuthenticated)
    const logout = useAuthStore( (state) => state.logout);
    const submit = useSubmit();
    const navigate = useNavigate();

    const handleLogout = useCallback(() => {
        logout();
        submit(null, {action:"/auth/logout", method:"post" });
        navigate('/auth/login', { replace: true });
        toast.info("Cierre de Sesion")
    }, [logout, submit, navigate]);
 
    useEffect( () => {

        if (!token || !isAuthenticated) {
            handleLogout();
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

    } , [token, isAuthenticated, handleLogout]);

    return (
        <div className="min-h-dvh bg-background">
            <Outlet />
        </div>
    );
}