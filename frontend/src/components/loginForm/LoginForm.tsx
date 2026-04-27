

export default function LoginForm() {
    return(
        <div className="grid grid-cols-3 grid-rows-3 gap-4">
           <form action="">
                <label htmlFor="username">Nombre de usuario:</label>
                <input type="text" name='username' id='username' placeholder='Usuario'/>
                <label htmlFor="password"> Contraseña:</label>
                <input type="password" name='password' id='password' placeholder='Contraseña'/>
                <button >¿No tienes cuenta? Registrate</button>
                <button type="submit">Inicia Sesión</button>
            </form> 
        </div>
        
    );
}