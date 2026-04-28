import React, { useState } from "react";
import { Link } from "react-router-dom";
import { LoginRequest } from "../../types";
import useAuthStore from "../../store/authStore";
import { login } from "../../services/authServices";


export default function LoginForm() {
    const [credentials, setCredentials] = useState<LoginRequest>({username:"", password:""});
    const setToken = useAuthStore( (state) => state.setToken);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await login(credentials);
            setToken(response.token);
            console.log('Login exitoso');
        } catch(err) {
            throw new Error('Error al ingresar los datos');
            console.log(err);
        }
        
    }

    return(
        <div >
           <form onSubmit={handleSubmit}className="grid flex justify-center ">
                
                    <label htmlFor="username">Nombre de usuario:</label>
                    <input 
                        type="text" 
                        name='username' 
                        id='username' 
                        onChange={(e) => setCredentials({...credentials, username: e.target.value})}
                        placeholder='Usuario'/>
                
                
                    <label htmlFor="password"> Contraseña:</label>
                    <input 
                        type="password" 
                        name='password' 
                        id='password' 
                        onChange={(e) => setCredentials({...credentials, password: e.target.value})}
                        placeholder='Contraseña'/>
                
                    <Link to='/auth/register'>¿No tienes cuenta? Registrate</Link>
                
                <button type="submit">Inicia Sesión</button>
            </form> 
        </div>
        
    );
}