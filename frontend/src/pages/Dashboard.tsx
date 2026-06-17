import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { RiGroupLine, RiLogoutBoxLine } from "@remixicon/react";
import ComboboxBasic from '../components/SessionLobby/VotingTypeCombobox'
import { Button } from "@/components/ui/button";
import { Link, useNavigate, useSubmit } from "react-router-dom";
import useAuthStore from "@/store/authStore";

export default function Dashboard (){
    const submit = useSubmit();
    const navigate = useNavigate();
    function handleLogout() {
        submit(null, {action:"/auth/logout", method:"post"});
        navigate('/', { replace: true });
    } 
    const username = useAuthStore( (state) => state.username);
    const tokenDuration = useAuthStore( (state) => state.tokenDuration);
    return(
        <div className="flex min-h-dvh flex-col items-center justify-center px-4 py-12">
                <Card className="w-full max-w-md">
                    <CardContent className="flex flex-col items-center gap-4 py-8">
                        <div className="flex size-16 items-center justify-center rounded-full bg-primary/10">
                            <RiGroupLine className="size-8 text-primary" />
                        </div>
                        <h1 className="text-2xl font-heading font-bold">Bienvenido, {username}</h1>
                        <p className="text-sm text-muted-foreground">Tu sesión está activa</p>
                        <p className="text-xs text-muted-foreground">Sesión expira en: {tokenDuration}</p>
                        <Separator />
                        <ComboboxBasic/>
                        <Button variant="outline" className="w-full">
                            <Link to='/SessionLobby' >Página sesiones</Link>
                        </Button>
                        <Button variant="outline" onClick={handleLogout} className="w-full">
                            <RiLogoutBoxLine className="size-4" />
                            Cerrar Sesión
                        </Button>
                    </CardContent>
                </Card>
            </div>
    )
}
