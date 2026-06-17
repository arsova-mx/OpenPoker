import { Link } from "react-router-dom";
import { Button } from "../ui/button";
import { Input } from "@base-ui/react";


export default function SessionFormJoin() {

    return(
        <>
            <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
                        <Input 
                            placeholder="Busca una Sesión"
                        />
                        <Button asChild variant="outline" size="lg" className="w-full sm:w-auto">
                            <Link to="/auth/login">Unirse a Sesion</Link>
                        </Button>
            </div>
        </>
    );
    
};