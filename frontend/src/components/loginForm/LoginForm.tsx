
import { useForm, SubmitHandler } from "react-hook-form"
import useAuthStore from "@/store/authStore"
import { authService } from "@/services/authService";
import {  Link, useSubmit } from "react-router-dom";

type formLogin = {
    username: string,
    password: string
}

export default function LoginForm() {
    const submit = useSubmit()
    
    const loginState = useAuthStore( (state) => state.login);
    const setTokenState = useAuthStore( (state) => state.setToken)

    const login = authService.login;
    const setToken = authService.saveToken;
   
    
    const {
        register, handleSubmit, formState: {errors}
    } = useForm<formLogin>()

    const onSubmit: SubmitHandler<formLogin> = async (data) => {
        const response = await login(data);

        if(response.token !== null) {
            const token = response.token;
            setToken(token);
            const tokenDuration = localStorage.getItem("tokenDuration") ?? '';
            
            setTokenState(token, tokenDuration);
            loginState(data.username, data.password);
            
        }
        submit(null, {action:"/auth",method: 'post'});
    }


    return (
        <div className="flex justify-center">
            
            <form onSubmit={handleSubmit(onSubmit)} className="flex justify-center flex-col">
                <label htmlFor="username">Usuario</label>
                <input type="text" 
                    {...register(
                        "username", 
                        {   
                            required: "Nombre de usuario requerido",
                        }
                    )} 
                    id="username"
                    className="rounded-2xl border bg-white"
                    placeholder="Nombre de usuario"
                />
                {errors.username ? <span className="text-red-800">{errors.username.message}</span> : <br />}
                
                <label htmlFor="password">Contraseña</label>
                <input type="password" 
                    {...register(
                        "password", 
                        {
                            required: "Contraseña requerida",
                            minLength: {
                                value: 6,
                                message: "La contraseña debe tener al menos 6 caracteres"
                            }
                        }
                    )}
                    id="password" 
                    placeholder="Contraseña"
                    className="border rounded-2xl bg-white"
                />
                
                {errors.password ? <span className="text-red-800">{errors.password.message}</span> : <br />}
                <p className="text-center">¿No tienes cuenta? <Link to='/auth/register' className="hover:underline">Registrate</Link></p>
                

                <button type="submit" 
                className="border rounded-2xl bg-emerald-700 text-white px-10 hover:cursor-pointer "
                >
                    Iniciar Sesion
                </button>

            </form>
        </div>
    )
}





