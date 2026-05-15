
import { Outlet, useLoaderData, useNavigate, useSubmit } from "react-router-dom";
import { useEffect } from "react";
import { getTokenDuration } from "@/hooks/useTokenDuration";
import useAuthStore from "@/store/authStore";
import { toast } from "sonner";

export default function Root() {
    const token = useLoaderData();
    const isAuthenticated = useAuthStore( (state) => state.isAuthenticated)
    const submit = useSubmit();
    const navigate = useNavigate();
 
    useEffect( () => {

        if (!token || !isAuthenticated) {
            return;
        }else if (token === 'EXPIRED') {
            submit(null, {action:"/auth/logout", method:"post" });
            toast.info("Sesion cerrada. Token Expirado ")
            return;
        }else {
            const timeRemaining = getTokenDuration();

            setTimeout( () => {
                submit(null, {action:"/auth/logout", method:"post" });
                navigate('/auth/login', { replace: true });
                toast.info("Sesion cerrada.")
            }, timeRemaining);
        }

    }, [token]);

    return (
        <div className="min-h-dvh bg-background">
            <Outlet />
        </div>
    );
}