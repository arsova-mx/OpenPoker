
import { useForm, Controller } from "react-hook-form"
import useAuthStore from "@/store/authStore"
import { authService } from "@/services/authService";
import { Link, useSubmit } from "react-router-dom";
import { Input } from "@/components/ui/input-ref";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

import { toast } from "sonner";
import * as z from "zod"
import { zodResolver } from "@hookform/resolvers/zod";
import { Field, FieldError, FieldLabel } from "../ui/field";


export default function LoginForm() {
    const submit = useSubmit()
    
    const loginState = useAuthStore( (state) => state.login);
    const setTokenState = useAuthStore( (state) => state.setToken)

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
    console.log(response);

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

