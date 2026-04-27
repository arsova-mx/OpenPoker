

export default function RegisterForm() {
    return(
        <form action="" className="main">
            <label htmlFor="email"> Correo:</label>
            <input type="text" name='email' id='email' placeholder='Correo'/>

            <label htmlFor="username">Nombre de usuario:</label>
            <input type="text" name='username' id='username' placeholder='Usuario'/>

            <label htmlFor="password"> Contraseña:</label>
            <input type="password" name='password' id='password' placeholder='Contraseña'/>

            <label htmlFor="reppassword"> Repite contraseña:</label>
            <input type="reppassword" name='reppassword' id='reppassword' placeholder='Repite contraseña'/>

            <button >¿Ya tienes cuenta? Iniciar sesión</button>
            <button type="submit">Crear cuenta</button>
        </form>
    );
}