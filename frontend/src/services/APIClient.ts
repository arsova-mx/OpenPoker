/**
 * Placeholder module for API client services.
 *
 * Services to be created during development:
 *  - sessionService  – REST calls for session CRUD
 *  - storyService    – REST calls for user story management
 *  - voteService     – REST calls for vote submission & reveal
 *  - apiClient       – base Axios/Fetch wrapper (base URL, interceptors)
 */


import axios from "axios";
import { toast } from "sonner";

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api';


export const instance = axios.create({
    baseURL: API_URL,
    headers: {
    'Content-Type': 'application/json',
  },
});

instance.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
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
    const message = error.response?.data || "Ocurrió un error inesperado";
    
    // El toast aparecerá en cualquier parte del proyecto
    toast.error("Error de servidor", {
      description: message,
    });

    return Promise.reject(error);
  }
);

export default instance;