import { Link, useSubmit } from "react-router-dom";
import { useForm, SubmitHandler } from "react-hook-form";
import { authService } from "@/services/authService";
import  * as z from 'zod'
import { zodResolver } from "@hookform/resolvers/zod";
import useAuthStore from "@/store/authStore";

const registerSchema = z.object({
    username: z.string().min(3, "El nombre de usuario debe tener al menos 3 caracteres"),
    email: z.email("Email invalido"),
    password: z.string().min(6, "La contraseña debe tener al menos 6 caracteres"),
    confirmPassword: z.string().min(6, "La contraseña debe tener al menos 6 caracteres")
}).refine( (data) => data.password === data.confirmPassword, { 
    message: "Las contraseñas no coinciden",
    path: ["confirmPassword"],
});

type registerFormData = z.infer<typeof registerSchema>

export default function RegisterForm() {
    const authRegister = authService.register;
    const submit = useSubmit();
    const login = authService.login;
    const setToken =authService.saveToken
    const setTokenState = useAuthStore( (state) => state.setToken);
    const loginState = useAuthStore( (state) => state.login);

    const { 
        register, handleSubmit, formState: {errors}
    } = useForm<registerFormData>({
        resolver: zodResolver(registerSchema),
        mode: "onTouched"
    }) 
    
    
    const onSubmit: SubmitHandler<registerFormData> = async(data) => {
        const dataForm = {
            username: data.username, 
            email: data.email, 
            password: data.password
        }
        const response = await authRegister(dataForm);

        if(!response.token !== null) {
            const loginForm = { username: data.username, password: data.password}
            login(loginForm);

            const token = response.token;
            setToken(token);
            const tokenDuration = localStorage.getItem("tokenDuration") ?? '';
            
            setTokenState(token, tokenDuration);
            loginState(data.username, data.password);

            submit(null, { action:'/auth', method: 'post'});
        }
        
        
    }

    return(
        <div className="">
            <form onSubmit={handleSubmit(onSubmit)} className="grid flex justify-center">
                <label htmlFor="email">Correo:</label>
                <input type="email" 
                    {
                        ...register(
                            "email",
                            {
                                required: "Email requerido",
                            }
                        )
                    }
                    className="rounded-2xl border bg-white"
                    id='email' 
                    placeholder='Correo'
                />
                {errors.email ? <span className="text-red-800">{errors.email.message}</span> : <br />}

                <label htmlFor="username">Nombre de usuario:</label>
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

                <label htmlFor="password"> Contraseña:</label>
                <input type="password" 
                    {...register(
                        "password", 
                        {
                            required: "Contraseña requerida",
                        }
                    )}
                    id="username"
                    placeholder="Contraseña" 
                    className="border rounded-2xl bg-white"
                />
                {errors.password ? <span className="text-red-800">{errors.password.message}</span> : <br />}

                <label htmlFor="confirmPassword"> Repita la contraseña:</label>
                <input type="password" 
                    {...register(
                        "confirmPassword", 
                        {
                            required: "Repita la contraseña",
                        }
                    )}
                    id="username" 
                    placeholder="Contraseña"
                    className="border rounded-2xl bg-white"
                />
                {errors.confirmPassword ? <span className="text-red-800">{errors.confirmPassword.message}</span> : <br />}
                <p className="text-center">¿Ya tienes cuenta? <Link to='/auth/login' className="hover:underline">
                    Iniciar sesión
                    </Link>
                </p>

                <button type="submit" className="border rounded-2xl bg-emerald-700 text-white px-10">Crear cuenta</button>
            </form>
        </div>
        
    );
}