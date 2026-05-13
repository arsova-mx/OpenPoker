
import { useForm, Controller } from "react-hook-form"
import useAuthStore from "@/store/authStore"
import { authService } from "@/services/authService";
import { Link, useSubmit } from "react-router-dom";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

import * as z from "zod"
import { zodResolver } from "@hookform/resolvers/zod";
import { Field, FieldError, FieldLabel } from "../ui/field";
import { PasswordInput } from "../ui/PasswordInput";


export default function LoginForm() {
    const submit = useSubmit()
    
    const loginState = useAuthStore( (state) => state.login);

    const login = authService.login;
    const setToken = authService.saveToken;
   
    const formSchema = z.object({
    username: z
        .string()
        .min(1, "Ingrese un Nombre de usuario"),
    password: z
        .string()
        .min(6, "La contraseña debe tener al menos 6 caracteres"),
    })

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        mode: "onTouched",
        defaultValues: {
            username: "",
            password: "",
        },
    });

    async function onSubmit(data: z.infer<typeof formSchema>) {

        const response = await login(data);

        if(response.token !== null) {
            const token = response.token;
            setToken(token);
            
            const tokenDuration = localStorage.getItem("tokenDuration") ?? '';
            
            loginState(data.username, token, tokenDuration);
            submit(null, {action:"/auth",method: 'post'});
        }
    }

    return (
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
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
                        <Field data-invalid={fieldState.invalid}>
                            <FieldLabel htmlFor="password">Contraseña</FieldLabel>
                            <PasswordInput 
                                {...field} 
                                id="password"
                                placeholder="Contraseña"
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

