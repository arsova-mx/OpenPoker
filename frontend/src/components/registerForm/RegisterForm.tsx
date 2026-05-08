import { Link, useSubmit } from "react-router-dom";
import { useForm, SubmitHandler } from "react-hook-form";
import { authService } from "@/services/authService";
import  * as z from 'zod'
import { zodResolver } from "@hookform/resolvers/zod";
import useAuthStore from "@/store/authStore";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

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
    const setToken = authService.saveToken
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

        if(response.token !== null) {
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
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
                <Label htmlFor="email">Correo</Label>
                <Input 
                    type="email" 
                    {...register("email", { required: "Email requerido" })}
                    id="email" 
                    placeholder="correo@ejemplo.com"
                    aria-invalid={!!errors.email}
                />
                {errors.email && (
                    <p className="text-sm text-destructive">{errors.email.message}</p>
                )}
            </div>

            <div className="flex flex-col gap-2">
                <Label htmlFor="username">Nombre de usuario</Label>
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
                    {...register("password", { required: "Contraseña requerida" })}
                    id="password" 
                    placeholder="Contraseña"
                    aria-invalid={!!errors.password}
                />
                {errors.password && (
                    <p className="text-sm text-destructive">{errors.password.message}</p>
                )}
            </div>

            <div className="flex flex-col gap-2">
                <Label htmlFor="confirmPassword">Repite la contraseña</Label>
                <Input 
                    type="password" 
                    {...register("confirmPassword", { required: "Repita la contraseña" })}
                    id="confirmPassword" 
                    placeholder="Contraseña"
                    aria-invalid={!!errors.confirmPassword}
                />
                {errors.confirmPassword && (
                    <p className="text-sm text-destructive">{errors.confirmPassword.message}</p>
                )}
            </div>

            <Button type="submit" className="w-full mt-2">
                Crear cuenta
            </Button>

            <p className="text-center text-sm text-muted-foreground">
                ¿Ya tienes cuenta?{" "}
                <Link to="/auth/login" className="text-primary font-medium hover:underline">
                    Iniciar sesión
                </Link>
            </p>
        </form>
    );
}