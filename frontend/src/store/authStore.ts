import { redirect } from "react-router-dom";
import { create } from "zustand";
import { persist } from "zustand/middleware";

interface AuthState {
    isAuthenticated: boolean | null;
    username: string | null;
    token: string | null;
    tokenDuration: number | null;
    email: string | null;
    login: (username: string) => void;
    logout: () => void;
    setToken: (token: string, tokenDuration: number) => void
}

const useAuthStore = create<AuthState>() ( 
    persist( (set) => ({
    isAuthenticated: false,
    email: null,
    username: null,
    token: null,
    tokenDuration: null,

    login: (username: string) => {
        set({ username: username })
        
    },

    setToken: (token: string, tokenDuration: number) => {
        set({token: token, tokenDuration: tokenDuration, isAuthenticated: true})
    },

    logout: () => {
        set({username: null, token: null, tokenDuration: null, isAuthenticated: false})
        
    },

    register: (username: string, email: string) => {
        set({ username, email});
    }
    }), {   
        name: 'auth',
        partialize: (state) => ({
            token: state.token, 
            tokenDuration: state.tokenDuration,
            username: state.username,
        }),
        

    }
    )
)

export default useAuthStore;
