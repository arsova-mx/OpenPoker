import { Link } from "react-router-dom";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function NotFound() {
    return (
        <div className="flex min-h-dvh items-center justify-center bg-background p-6">
            <Card className="w-full max-w-md text-center">
                <CardHeader>
                    <p className="text-5xl font-bold text-muted-foreground" aria-hidden="true">404</p>
                    <CardTitle className="text-2xl font-heading">Página no encontrada</CardTitle>
                </CardHeader>
                <CardContent>
                    <p className="text-sm text-muted-foreground">
                        La página que buscas no existe o fue movida.
                    </p>
                </CardContent>
                <CardFooter className="justify-center">
                    <Button asChild variant="outline">
                        <Link to="/">Volver al inicio</Link>
                    </Button>
                </CardFooter>
            </Card>
        </div>
    );
}
