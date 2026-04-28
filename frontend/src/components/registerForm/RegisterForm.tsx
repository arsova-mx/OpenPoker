import { Link } from "react-router-dom";


export default function RegisterForm() {

    
    const handleSubmit = () => {

    }

    return(
        <div className="">
            <form onSubmit={handleSubmit} className="grid flex justify-center">
                <label htmlFor="email"> Correo:</label>
                <input type="text" name='email' id='email' placeholder='Correo'/>

                <label htmlFor="username">Nombre de usuario:</label>
                <input type="text" name='username' id='username' placeholder='Usuario'/>

                <label htmlFor="password"> Contraseña:</label>
                <input type="password" name='password' id='password' placeholder='Contraseña'/>

                <label htmlFor="reppassword"> Repite contraseña:</label>
                <input type="reppassword" name='reppassword' id='reppassword' placeholder='Repite contraseña'/>

                <Link to='/auth/login' >¿Ya tienes cuenta? Iniciar sesión</Link>
                <button type="submit">Crear cuenta</button>
            </form>
        </div>
        
    );
}