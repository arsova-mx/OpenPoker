import { Link } from "react-router-dom";
import { Field, FieldDescription, FieldError,FieldGroup, FieldLabel, FieldLegend, FieldSet} from "@/components/ui/field";
import { Card, CardContent } from "../ui/card";
import { Input } from "../ui/input";
import { Button } from "../ui/button";
import { toast } from "sonner";
import { useForm, Controller } from "react-hook-form"
import * as z from 'zod';
import { zodResolver } from '@hookform/resolvers/zod'
import { login } from "@/services/authService";
import useAuthStore from "@/store/authStore";

const authSchema = z.object({
    username: z.string().min(1, "Usuario requerido"),
    password: z.string().min(6, "La contraseña debe tener al menos 6 caracteres"),
})

export default function LoginForm() {

    const form = useForm<z.infer<typeof authSchema>>({
        resolver: zodResolver(authSchema),
        defaultValues: {
            username: '',
            password: ''
        }
    });
    
    function onSubmit(data: z.infer<typeof authSchema>) {

        toast("You submitted the following values:", {
            description: (
                <pre className="mt-2 w-[320px] overflow-x-auto rounded-md bg-code p-4 text-code-foreground">
                    <code>{JSON.stringify(data, null, 2)}</code>
                </pre>
            ),
            position: "bottom-right",
            classNames: {
                content: "flex flex-col gap-2",
            },
            style: {
                "--border-radius": "calc(var(--radius)  + 4px)",
            } as React.CSSProperties,
        })

        const username = data.username;
        const password = data.password;
        const loginStore = useAuthStore( (state) => state.login)

        
        console.log(data)
        loginStore(username,password)
        login({username, password});

    }

    return(
        <div className=" flex justify-center ">
            <Card >
                <CardContent>
                    <form id="authForm" onSubmit={form.handleSubmit(onSubmit)}>
                    
                        <FieldLegend> Inicio de sesión</FieldLegend>
                        <FieldDescription> Inicia sesion con correo y contraseña</FieldDescription>
                        <FieldGroup>
                            <Controller 
                                name="username"
                                control={form.control}
                                render= { ({field, fieldState}) => (
                                    <Field data-invalid={fieldState.invalid}>
                                        <FieldLabel htmlFor="username">Nombre de usuario: </FieldLabel>
                                        <Input 
                                            {...field} 
                                            id="username"
                                            aria-invalid={fieldState.invalid}
                                            type="text"
                                            placeholder="Usuario"
                                        />
                                        {fieldState.invalid && <FieldError errors={[fieldState.error]}/>}
                                    </Field>
                                )
                                }
                            />
                            <Controller
                                name="password"
                                control={form.control}
                                render={ ({field, fieldState}) => (
                                    <Field>
                                        <FieldLabel htmlFor="password">Contraseña:</FieldLabel>
                                        <Input 
                                            {...field}  
                                            id="password"
                                            type="password"
                                            placeholder="Contraseña"
                                            aria-invalid={fieldState.invalid}
                                            autoComplete="off"
                                        />
                                        {fieldState.invalid && (<FieldError errors={[fieldState.error]}/>)}
                                    </Field>
                                )}
                            />
                            <Field orientation="horizontal">
                                <Button><Link to='/auth/register'>¿No tienes cuenta? Registrate</Link></Button>
                            </Field>
                            <Button type="submit" form="authForm">Iniciar sesion</Button>
                        </FieldGroup>
                    
                    </form>
                </CardContent>

            </Card>


           
        </div>
        
    );
}