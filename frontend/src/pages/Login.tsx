
import LoginForm from "../components/loginForm/LoginForm";

export default function Login() {
    return (
        <div className="flex flex-col gap-6">
            <div className="text-center lg:text-left">
                <h1 className="text-2xl font-heading font-bold tracking-tight text-foreground">
                    Iniciar sesión
                </h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Ingresa tus credenciales para volver a estimar con tu equipo
                </p>
            </div>
            <LoginForm />
        </div>
    );
}

