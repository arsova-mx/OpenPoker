
import { create } from "zustand";

interface AuthState {
    isAuthenticated: boolean | null;
    username: string | null;
    password: string | null;
    token: string | null;
    tokenDuration: string | null;
    email: string | null;
    login: (username: string, password: string) => void;
    logout: () => void;
    setToken: (token: string, tokenDuration: string) => void
}

const useAuthStore = create<AuthState>() ( 
    (set) => ({
    isAuthenticated: false,
    password: null,
    email: null,
    username: null,
    token: null,
    tokenDuration: null,

    login: (username: string, password: string) => {
        set({ username: username, password: password })
        
    },

    setToken: (token: string, tokenDuration: string | null) => {
        set({token: token, tokenDuration: tokenDuration, isAuthenticated: true})
    },

    logout: () => {
        set({username: null, token: null, tokenDuration: null, isAuthenticated: false})
        
    },

    register: (username: string, email: string) => {
        set({ username, email});
    }
    })
    )


export default useAuthStore;
