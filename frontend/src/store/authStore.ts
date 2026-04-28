import { create } from "zustand";
import { persist } from "zustand/middleware";

interface AuthState {
    username: string | null;
    token: string | null;
    tokenDuration: number | null;
    email: string | null;
    password: string | null;
    login: (username: string, password: string) => void;
    logout: () => void;
    setToken: (token: string) => void
}

const useAuthStore = create<AuthState>()( 
    persist( (set) => ({
    email: null,
    username: null,
    password: null,
    token: null,
    tokenDuration: null,

    login: (username: string, password: string) => {
        set({ username: username, password: password })
    },

    setToken: (token: string) => {
        set({token: token})
    },

    logout: () => {
        set({username: null, token: null, tokenDuration: null})
    },

    register: (username: string, email: string, password: string) => {
        set({ username, email, password });
    }
    }), {   
        name: 'token',
        partialize: (state) => ({
            token: state.token, 
            tokenDuration: state.tokenDuration,
            username: state.username
        }),
    } 
    )
)

export default useAuthStore;
