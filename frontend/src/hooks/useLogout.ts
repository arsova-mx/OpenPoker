import { useCallback } from "react";
import { useNavigate } from "react-router-dom";
import useAuthStore from "@/store/authStore";
import { authService } from "@/api/services/authService";

/** Cierra la sesión: limpia el token guardado, el store de auth y redirige al login. */
export function useLogout() {
  const navigate = useNavigate();
  const clearStore = useAuthStore((state) => state.logout);

  return useCallback(() => {
    authService.logout();
    clearStore();
    navigate("/auth/login", { replace: true });
  }, [clearStore, navigate]);
}

export default useLogout;
