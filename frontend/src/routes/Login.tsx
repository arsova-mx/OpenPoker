import { Navigate, useNavigate } from "react-router-dom";
import LoginForm from "../components/loginForm/LoginForm1";
import { BugReportForm } from "@/components/BugReportForm";

export default function Login() {
    return (
        <div className="rounded-xl m-10 bg-emerald-200 hover:shadow-xl/50 shadow-xl">
            <h1>Página de Inicio de sesión</h1>
            <LoginForm/>
        </div>
    );
}

export async function action(){
    const navigate = useNavigate()
    return navigate('/', { replace: true });
}