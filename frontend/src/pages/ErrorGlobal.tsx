
import { useEffect } from "react";
import { Link, useRouteError, isRouteErrorResponse } from "react-router-dom";
import { toast } from "sonner";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

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
        <div className="flex min-h-dvh items-center justify-center bg-background p-6">
            <Card className="w-full max-w-md text-center">
                <CardHeader>
                    <CardTitle className="text-2xl font-heading">Algo salió mal</CardTitle>
                </CardHeader>
                <CardContent className="flex flex-col gap-4">
                    <p className="text-sm text-muted-foreground">
                        Lo sentimos, ha ocurrido un error inesperado.
                    </p>
                    <div className="rounded-lg bg-destructive/10 p-4 text-sm text-destructive">
                        {isRouteErrorResponse(error) ? (
                            <p>
                                <strong>Status:</strong> {error.status} <br />
                                <strong>{error.statusText || error.data?.message}</strong>
                            </p>
                        ) : (
                            <p>
                                <strong>Error:</strong> {errorMessage}
                            </p>
                        )}
                    </div>
                </CardContent>
                <CardFooter className="justify-center">
                    <Button asChild variant="outline">
                        <Link to="/">Volver al Inicio</Link>
                    </Button>
                </CardFooter>
            </Card>
        </div>
    )
}

export default ErrorGlobal;