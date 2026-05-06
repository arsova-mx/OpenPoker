
import { useEffect } from "react";
import { Link, useRouteError } from "react-router-dom";
import { toast } from "sonner";

function ErrorGlobal() {
    const error = useRouteError() as any;

    useEffect( () => {
        toast.error("Error: ", {
            description: error.statusText || error.message 
        });
    }, [error]);

    return(
        <div className="p-10 text-center">
            <h1>Esto no deberia pasar. Algo slaio mal</h1>
            <p>En un momento lo resolvemos</p>
            <Link to="/">Volver a inicio</Link>
        </div>
    )
}

export default ErrorGlobal;