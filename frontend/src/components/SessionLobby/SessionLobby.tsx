import { Link } from "react-router-dom";


import { Button } from "../ui/button";
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarHeader,
} from "../ui/sidebar"

import { RiGroupLine, RiSendPlaneLine, RiBarChartBoxLine, RiLogoutBoxLine} from '@remixicon/react'
import { Input } from "../ui/input";

function FeatureStep({ icon, step, title, description }: { icon: React.ReactNode; step: number; title: string; description: string }) {
    return (
        <div className="flex flex-col items-center gap-2 text-center">
            <div className="flex size-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                {icon}
            </div>
            <span className="text-xs font-medium text-muted-foreground">Paso {step}</span>
            <h3 className="text-sm font-semibold text-foreground">{title}</h3>
            <p className="text-xs text-muted-foreground leading-relaxed">{description}</p>
        </div>
    );
}

export default function SessionLobby() {
    return (
        <>
            <div className="flex min-h-dvh flex-col">
            {/* Hero */}
            <section className="flex flex-1 flex-col items-center justify-center px-4 py-16 text-center md:py-24">
                <div className="mx-auto max-w-2xl">
                    <h1 className="text-4xl font-heading font-bold tracking-tight text-foreground md:text-5xl lg:text-6xl">
                        Pagina de sesiones
                        
                    </h1>
                    <p className="mt-4 text-base text-muted-foreground md:text-lg">
                        Crea o únete a una sesion ahora
                    </p>

                    <Input 
                        placeholder="Nombre de la sesion"
                    />

                    <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
                        <Button asChild size="lg" className="w-full sm:w-auto">
                            <Link to="auth/register">Crear Sesion</Link>
                        </Button>
                        <Button asChild variant="outline" size="lg" className="w-full sm:w-auto">
                            <Link to="auth/login">Unirse a Sesion</Link>
                        </Button>
                    </div>
                </div>
            </section>

            {/* Footer */}
            <footer className="border-t px-4 py-6 text-center text-xs text-muted-foreground">
                Open<span className="font-semibold text-foreground">Poker</span> — Open Source Planning Poker
            </footer>
        </div>
        </>
    )
}