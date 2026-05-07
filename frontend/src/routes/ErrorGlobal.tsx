
import { useEffect } from "react";
import { Link, useRouteError, isRouteErrorResponse } from "react-router-dom";
import { toast } from "sonner";

function ErrorGlobal() {
  
    const error = useRouteError()
    let errorMessage: string;

    if( isRouteErrorResponse(error)) {
      errorMessage = error.statusText || error.data?.message;
    } else if( error instanceof Error) {
      errorMessage = error.message;
    }else if(typeof error === 'string') {
      errorMessage = error
    } else {
      errorMessage = "Error desconocido"
    }

    useEffect( () => {
        toast.error("Error: ", {
            description: errorMessage
        });
    }, [errorMessage]);

    return(
        <div className="p-40 text-center ">
        
      <h1>Algo salió mal</h1>
      <p>Lo sentimos, ha ocurrido un error inesperado.</p>
      <br/>
      <div className="bg-emerald-200 p-64 rounded-lg inline-block hover:shadow-xl/50">
        
        {isRouteErrorResponse(error) ? (
          <p>
            <strong>Status:</strong> {error.status} <br />
            <strong> {error.statusText || error.data?.message}</strong>
          </p>
        ) : (
          <p>
            <strong>Error:</strong> {errorMessage}
          </p>
        )}
      </div>

      <div className="pt-10">
        <Link to="/" className="hover:bg-emerald-900 hover:text-gray-50 p-2 rounded-lg"> 
            Volver al Inicio
        </Link>
            
         
        
      </div>
    </div>
    )
}

export default ErrorGlobal;