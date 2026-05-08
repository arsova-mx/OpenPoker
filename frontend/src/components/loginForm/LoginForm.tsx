
import { useForm, SubmitHandler } from "react-hook-form"
import useAuthStore from "@/store/authStore"
import { authService } from "@/services/authService";
import { Link, useSubmit } from "react-router-dom";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

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
            submit(null, {action:"/auth",method: 'post'});
        }
    }


    return (
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
                <Label htmlFor="username">Usuario</Label>
                <Input 
                    type="text" 
                    {...register("username", { required: "Nombre de usuario requerido" })} 
                    id="username"
                    placeholder="Nombre de usuario"
                    aria-invalid={!!errors.username}
                />
                {errors.username && (
                    <p className="text-sm text-destructive">{errors.username.message}</p>
                )}
            </div>

            <div className="flex flex-col gap-2">
                <Label htmlFor="password">Contraseña</Label>
                <Input 
                    type="password" 
                    {...register("password", {
                        required: "Contraseña requerida",
                        minLength: {
                            value: 6,
                            message: "La contraseña debe tener al menos 6 caracteres"
                        }
                    })}
                    id="password" 
                    placeholder="Contraseña"
                    aria-invalid={!!errors.password}
                />
                {errors.password && (
                    <p className="text-sm text-destructive">{errors.password.message}</p>
                )}
            </div>

            <Button type="submit" className="w-full mt-2">
                Iniciar Sesión
            </Button>

            <p className="text-center text-sm text-muted-foreground">
                ¿No tienes cuenta?{" "}
                <Link to="/auth/register" className="text-primary font-medium hover:underline">
                    Regístrate
                </Link>
            </p>
        </form>
    )
}

