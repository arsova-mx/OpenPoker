import { LoginRequest, RegisterRequest, AuthResponse } from "../types";
import { instance } from './APIClient'
import useAuthStore from "../store/authStore";
import axios from "axios";
import { useNavigate } from "react-router-dom";


const login = async (data: LoginRequest): Promise<AuthResponse> => {
    try {
        const response = await instance.post<AuthResponse>('/auth/login', data)
        console.log("Login exitoso");
        localStorage.setItem('token', response.data.token);
        
        return response.data;
        
    } catch (error) {
        if (axios.isAxiosError(error)){
            const serverMessage = error.response?.data
            throw new Error (serverMessage || "Error de autenticación")
        }
        throw new Error('Ocurrió un error inesperado al conectar con el servidor');
    }
    
}

const register = async (data: RegisterRequest): Promise<AuthResponse> => {
    try {
        const response = await instance.post<AuthResponse>('/auth/register', data)
        return response.data
    } catch (error) {
        if (axios.isAxiosError(error)){
            const serverMessage = error.response?.data
            throw new Error (serverMessage || "Error de Registro")
        }
        throw new Error('Ocurrió un error inesperado al conectar con el servidor');
        
    }
}

const saveToken = (token : string) => {
    const navigate = useNavigate();
    localStorage.setItem("token", token);

    const tokenDuration = new Date();
    tokenDuration.setSeconds(tokenDuration.getSeconds()+10)
    localStorage.setItem('tokenDuration', tokenDuration.toISOString());
    navigate("/")
}

const getToken = () => {
    const local = localStorage.getItem('token');
    return local;
}

const logout = () => {
    const navigate = useNavigate();
    localStorage.clear();
    navigate("/");
}

export const authService = {
    
    login, register, saveToken, getToken, logout
}

export default authService;