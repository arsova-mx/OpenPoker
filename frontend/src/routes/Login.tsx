import LoginForm from "../components/loginForm/LoginForm";


export default function Login() {
    return (
        <div className="flex flex-col  p-10 min-h-dvh bg-amber-300">
            <h1>Página de Inicio de sesión</h1>
            <LoginForm/>
        </div>
    );
}