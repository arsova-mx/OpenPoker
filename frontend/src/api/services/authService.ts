import { LoginRequest, RegisterRequest, AuthResponse } from "../../types";
import { instance } from '../clients/APIClient'
import axios from "axios";
import { getErrorMessage } from "@/lib/errors";


const login = async (data: LoginRequest): Promise<AuthResponse> => {
    try {
        const response = await instance.post<AuthResponse>('/auth/login', data);
        return response.data;
    } catch (error) {
        if (axios.isAxiosError(error)){
            throw new Error(getErrorMessage(error, "Error de autenticación"))
        }
        throw new Error('Ocurrió un error inesperado al conectar con el servidor');
    }

}

const register = async (data: RegisterRequest): Promise<AuthResponse> => {
    try {
        const response = await instance.post<AuthResponse>('/auth/register', data);
        return response.data
    } catch (error) {
        if (axios.isAxiosError(error)){
            throw new Error(getErrorMessage(error, "Error de Registro"))
        }
        throw new Error('Ocurrió un error inesperado al conectar con el servidor');

    }
}

const saveToken = (token: string) => {
    if (!token) {
        throw new Error("El servidor no devolvió un token de sesión");
    }
    localStorage.setItem("token", token);
    const tokenDuration = new Date();
    tokenDuration.setSeconds(tokenDuration.getSeconds()+3600);
    localStorage.setItem('tokenDuration', tokenDuration.toISOString());
}

const getToken = () => {
    const local = localStorage.getItem('token');
    return local;
}

const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('tokenDuration');
}

export const authService = {
    login, register, saveToken, getToken, logout
}

export default authService;