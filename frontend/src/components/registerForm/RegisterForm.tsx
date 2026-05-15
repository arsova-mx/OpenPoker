import { Link, useSubmit } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import { useState } from "react";
import { authService } from "@/services/authService";
import * as z from 'zod'
import { zodResolver } from "@hookform/resolvers/zod";
import useAuthStore from "@/store/authStore";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

import { Field, FieldError, FieldLabel } from "../ui/field";
import { PasswordInput } from "../ui/PasswordInput";


const registerSchema = z.object({
        username: z
            .string()
            .trim()
            .min(1, "Ingrese un nombre de usuario"),
            .trim()
            .min(1, "Ingrese un nombre de usuario"),
        email: z
            .string()
            .trim()
            .email("Ingrese un email válido"),
        password: z
            .string()
            .trim()
            .min(6, "La contraseña debe tener al menos 6 caracteres"),
        confirmPassword: z
            .string()
            .trim()
            .min(6, "La contraseña debe tener al menos 6 caracteres"),
    }).refine( (data) => data.password === data.confirmPassword, { 
    message: "Las contraseñas no coinciden",
    path: ["confirmPassword"],
});

export default function RegisterForm() {

    const authRegister = authService.register;
    const submit = useSubmit();
    const saveToken = authService.saveToken
    const loginState = useAuthStore( (state) => state.login);
    const [serverError, setServerError] = useState<string | null>(null);

    const form = useForm<z.infer<typeof registerSchema>>({
            resolver: zodResolver(registerSchema),
            mode: "all",
            defaultValues: {
                username: "",
                email: "",
                password: "",
                confirmPassword: "",
            },
    });

    async function onSubmit(data: z.infer<typeof registerSchema>) {
        setServerError(null);
        try {
            const dataForm = {
                username: data.username, 
                email: data.email, 
                password: data.password
            }
            const response = await authRegister(dataForm);

            const token = response.token;
            saveToken(token);
            const tokenDuration = localStorage.getItem("tokenDuration") ?? '';
        
            loginState(data.username, token, tokenDuration);

            submit(null, { action:'/auth', method: 'post'});
        } catch (error) {
            const message = error instanceof Error ? error.message : "Error al registrar usuario";
            setServerError(message);
        }
    }

    return(
        <form onSubmit={form.handleSubmit(onSubmit)} className="flex flex-col gap-4">
            {serverError && (
                <p className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
                    {serverError}
                </p>
            )}
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
                                {...field} 
                                type="email"
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
                                {...field} 
                                id="username"
                                type="text"
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
                            <FieldLabel  htmlFor="password">Contraseña</FieldLabel >
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

            <div className="flex flex-col gap-2">
                <Controller 
                    name= "confirmPassword"
                    control= {form.control}
                    render={( ({field, fieldState}) => (
                        <Field data-invalid={fieldState.invalid}>
                            <FieldLabel htmlFor="confirmPassword">Confirmar contraseña</FieldLabel>
                            <PasswordInput 
                                {...field} 
                                id="confirmPassword"
                                placeholder="Repite la contraseña"
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