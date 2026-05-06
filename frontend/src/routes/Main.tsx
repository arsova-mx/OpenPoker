import useAuthStore from "../store/authStore";
import { Form, Link } from "react-router-dom";
export default function Main() {
    const username = useAuthStore( (state) => state.username)
    return(
        <div className="flex justify-center flex-col">
            <h1>Bienvenido {username} </h1>

            <Link to="auth/login">Inicia sesion</Link>
            <Link to="auth/register">Registrate</Link>
            <Form action="auth/logout" method="post">
                <button className="border rounded-2xl bg-emerald-700 text-white px-10">Cerrar Sesion</button>
            </Form>
        
        </div>
    );
}