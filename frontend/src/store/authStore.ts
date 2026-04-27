import { create } from "zustand";

const useAuthStore = create( (set) => ({
    username: null,
    password: null,
    token: null,
    tokenDuration: null
}))