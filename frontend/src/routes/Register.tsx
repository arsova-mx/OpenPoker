import RegisterForm from "../components/registerForm/RegisterForm";

export default function Register() {
    return (
        <div className="flex flex-col gap-6">
            <div className="text-center lg:text-left">
                <h1 className="text-2xl font-heading font-bold tracking-tight text-foreground">
                    Crear cuenta
                </h1>
                <p className="mt-1 text-sm text-muted-foreground">
                    Regístrate gratis y comienza a estimar con tu equipo
                </p>
            </div>
            <RegisterForm />
        </div>
    );
}