import axios from "axios";
import { toast } from "sonner";
import { getErrorMessage } from "@/lib/errors";
import { readStoredToken } from "@/hooks/useTokenDuration";

// Relativo por defecto: funciona con el proxy de Vite en desarrollo y con nginx en Docker/producción.
const API_URL = import.meta.env.VITE_API_URL || "/api";

export const instance = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

instance.interceptors.request.use(
    (config) => {
        const token = readStoredToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config
    },
    (error) => {
        return Promise.reject(error);
    }
)

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    // Los formularios de login y registro ya muestran el error en línea
    const isAuthRequest = error.config?.url?.startsWith("/auth/");
    if (!isAuthRequest) {
      toast.error("Error de servidor", {
        description: getErrorMessage(error),
      });
    }

    return Promise.reject(error);
  }
);

export default instance;
