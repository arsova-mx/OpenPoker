
import useAuthStore from "../store/authStore";
import { Link, useSubmit } from "react-router-dom";

export default function Main() {
    const submit = useSubmit()
    const username = useAuthStore( (state) => state.username)
    const isAuthenticated = useAuthStore( (state) => state.isAuthenticated)
    const tokenDuration = useAuthStore( (state) => state.tokenDuration)

    function handleLogout () {
            submit(null, {action:"/auth/logout", method:"post" });
    }

    return(
        <div className="flex text-center flex-col">
            {isAuthenticated ? 
            <>
                <h1>Bienvenido {username} </h1> 
                <p>Tiempo de Token: {tokenDuration}</p>
            </>
                
                : <h1>Página de inicio</h1>}
            {!isAuthenticated ?
            <>
                <Link to="auth/login" className="hover:underline">Inicia sesion</Link>
                <Link to="auth/register" className="hover:underline">Registrate</Link>
            </>
            
            :
                <button className=
                "border rounded-2xl bg-emerald-700 text-white px-10 hover:underline cursor-pointer hover:shadow-xl focus:bg-emerald-600 focus:cursor-not-allowed"
                onClick={() => handleLogout()}
            >
                Cerrar Sesion
            </button>
            }
            
            
        
        </div>
    );
}