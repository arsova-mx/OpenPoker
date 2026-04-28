import { LoginRequest, RegisterRequest, AuthResponse } from "../types";
import { instance } from './aplClient'
import useAuthStore from "../store/authStore";
import axios from "axios";

export const login = async (data: LoginRequest): Promise<AuthResponse> => {
    try {
        const response = await instance.post<AuthResponse>('/login', data)
        return response.data;

    } catch (error) {
        if (axios.isAxiosError(error)){
            const serverMessage = error.response?.data?.message;
            throw new Error (serverMessage || "Error de autenticación")
        }
        throw new Error('Ocurrió un error inesperado al conectar con el servidor');
    }
    
}

export const register = async (data: RegisterRequest): Promise<AuthResponse> => {
    try {
        const response = await instance.post<AuthResponse>('/register', data)
        return response.data
    } catch (err) {
        if (axios.isAxiosError(err)){
            const serverMessage = err.response?.data?.message;
            throw new Error (serverMessage || "Error de Registro")
        }
        throw new Error('Ocurrió un error inesperado al conectar con el servidor');
    }
}

export const saveToken = async (token : string) => {

}

export const getToken = () => {

}

export const logout = () => {

}