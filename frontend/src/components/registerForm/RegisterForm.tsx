import { Link, useSubmit } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { authService } from "@/services/authService";
import  * as z from 'zod'
import { zodResolver } from "@hookform/resolvers/zod";
import useAuthStore from "@/store/authStore";
import { Input } from "@/components/ui/input-ref";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

import { toast } from "sonner";
import { Field, FieldError, FieldLabel } from "../ui/field";


const registerSchema = z.object({
        username: z
            .string()
            .min(1, "Ingrese un Nombre de usuario"),
        email: z
            .email("Ingrese un email válido"),
        password: z
            .string()
            .min(6, "La contraseña debe tener al menos 6 caracteres"),
        confirmPassword: z
            .string()
            .min(6, "La contraseña debe tener al menos 6 caracteres"),
    }).refine( (data) => data.password === data.confirmPassword, { 
    message: "Las contraseñas no coinciden",
    path: ["confirmPassword"],
});

export default function RegisterForm() {
    const authRegister = authService.register;
    const submit = useSubmit();
    const login = authService.login;
    const setToken = authService.saveToken
    const setTokenState = useAuthStore( (state) => state.setToken);
    const loginState = useAuthStore( (state) => state.login);

    // const { 
    //     register, handleSubmit, formState: {errors}
    // } = useForm<registerFormData>({
    //     resolver: zodResolver(registerSchema),
    //     mode: "onTouched"
    // }) 

    

    const form = useForm<z.infer<typeof registerSchema>>({
            resolver: zodResolver(registerSchema),
            mode: "onTouched",
            defaultValues: {
                username: "",
                email: "",
                password: "",
            },
      });
    async function onSubmit(data: z.infer<typeof registerSchema>) {

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
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
                <Controller
                    name="email"
                    control= {form.control}
                    render={( ({field, fieldState}) => (
                        <Field data-invalid={fieldState.invalid}>
                            <FieldLabel  htmlFor="email">
                                Correo electronico
                            </FieldLabel >
                            <Input 
                                type="email"
                                {...field} 
                                id="email"
                                placeholder="Correo electronico"
                                aria-invalid={fieldState.invalid}
                            />
                            {fieldState.invalid && (
                                <FieldError errors={[fieldState.error]} />
                            )}
                        </Field>
                    ))
                    }
                />
            </div>

            <div className="flex flex-col gap-2">
                <Controller 
                    name= "username"
                    control= {form.control}
                    render={( ({field, fieldState}) => (
                        <Field data-invalid={fieldState.invalid}>
                            <FieldLabel  htmlFor="username">
                                Usuario
                            </FieldLabel >
                            <Input 
                                type="text"
                                {...field} 
                                id="username"
                                placeholder="Nombre de usuario"
                                aria-invalid={fieldState.invalid}
                            />
                            {fieldState.invalid && (
                                <FieldError errors={[fieldState.error]} />
                            )}
                        </Field>
                    ))
                    }
                />
            </div>

            <div className="flex flex-col gap-2">
                <Controller 
                    name= "password"
                    control= {form.control}
                    render={( ({field, fieldState}) => (
                        <>
                            <Label htmlFor="password">Contraseña</Label>
                            <Input 
                                type="password" 
                                {...field} 
                                id="password"
                                placeholder="Contraseña"
                                aria-invalid={fieldState.invalid}
                            />
                            {fieldState.invalid && (
                                <FieldError errors={[fieldState.error]} />
                            )}
                        </>
                    ))
                    }
                />
            </div>

            <div className="flex flex-col gap-2">
                <Controller 
                    name= "confirmPassword"
                    control= {form.control}
                    render={( ({field, fieldState}) => (
                        <>
                            <Label htmlFor="confirmPassword">Contraseña</Label>
                            <Input 
                                type="password" 
                                {...field} 
                                id="confirmPassword"
                                placeholder="Contraseña"
                                aria-invalid={fieldState.invalid}
                            />
                            {fieldState.invalid && (
                                <FieldError errors={[fieldState.error]} />
                            )}
                        </>
                    ))
                    }
                />
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