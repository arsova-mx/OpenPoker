

import useAuthStore from "@/store/authStore";
import SessionFormCreate from "./SessionFormCreate";
import SessionFormJoin from "./SessionFormJoin";
import { Button } from "../ui/button";


export default function SessionLobby() {
    const username = useAuthStore( (state) => state.username);
    return (
        <>
            <div className="">
                <div className="">
                    <h1 className="">
                        Bienvenido {username}
                    </h1>
                    <p className="mt-4 text-base text-muted-foreground md:text-lg">
                        Crea o únete a una sesión ahora
                    </p>
                    <p className="mt-4 text-base text-muted-foreground md:text">
                        Crea una sesión
                    </p>
                    <SessionFormCreate />
                    <p className="mt-4 text-base text-muted-foreground md:text">
                        Únete a una sesión
                    </p>
                    <SessionFormJoin />
                    <Button size="lg" className="w-full sm:w-auto">
                        Cerrar Sesión
                    </Button>
                </div>
            
        </div>
        </>
    )
}