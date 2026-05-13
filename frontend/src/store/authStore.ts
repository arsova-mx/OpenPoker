
import { create } from "zustand";
import { persist, createJSONStorage } from 'zustand/middleware'

interface AuthState {
    isAuthenticated: boolean | null;
    username: string | null;
    token: string | null;
    tokenDuration: string | null;
    email: string | null;
    login: (username: string, token: string, tokenDuration: string) => void;
    logout: () => void;
}

const useAuthStore = create<AuthState>() (
    persist (
        (set) => ({
            isAuthenticated: false,
            email: null,
            username: null,
            token: null,
            tokenDuration: null,
            login: (username: string, token: string, tokenDuration: string | null) => {
                set({ username: username, token: token, tokenDuration: tokenDuration, isAuthenticated: true })
            },
            logout: () => {
                set({username: null, token: null, tokenDuration: null, isAuthenticated: false})
            },
        }),
        {
            name: 'auth', // name of the item in the storage (must be unique)
            storage: createJSONStorage(() => sessionStorage),
            partialize: (state) => ({  
                isAuthenticated: state.isAuthenticated,
                tokenDuration: state.tokenDuration,
                username: state.username
            }),
        }
    )
)


export default useAuthStore;
