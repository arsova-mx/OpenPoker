
import useAuthStore from "../store/authStore";
import { Link, useNavigate, useSubmit } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { RiGroupLine, RiSendPlaneLine, RiBarChartBoxLine, RiLogoutBoxLine } from "@remixicon/react";


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

export default function Main() {
    
    const submit = useSubmit();
    const navigate = useNavigate();
    const username = useAuthStore( (state) => state.username);
    const isAuthenticated = useAuthStore( (state) => state.isAuthenticated);
    const tokenDuration = useAuthStore( (state) => state.tokenDuration);

    function handleLogout() {
        submit(null, {action:"/auth/logout", method:"post"});
        navigate('/', { replace: true });
    }

    if (isAuthenticated) {
        return (
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
                        <Button variant="outline" className="w-full">
                            <RiLogoutBoxLine className="size-4" />
                            Página sesiones
                        </Button>
                        <Button variant="outline" onClick={handleLogout} className="w-full">
                            <RiLogoutBoxLine className="size-4" />
                            Cerrar Sesión
                        </Button>
                    </CardContent>
                </Card>
            </div>
        );
    }

    return (
        <div className="flex min-h-dvh flex-col">
            {/* Hero */}
            <section className="flex flex-1 flex-col items-center justify-center px-4 py-16 text-center md:py-24">
                <div className="mx-auto max-w-2xl">
                    <h1 className="text-4xl font-heading font-bold tracking-tight text-foreground md:text-5xl lg:text-6xl">
                        Planning Poker
                        <br />
                        <span className="text-primary">para equipos ágiles</span>
                    </h1>
                    <p className="mt-4 text-base text-muted-foreground md:text-lg">
                        Estimaciones rápidas, consenso real. La alternativa open source para que tu equipo estime historias de usuario de forma divertida y eficiente.
                    </p>

                    <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center">
                        <Button asChild size="lg" className="w-full sm:w-auto">
                            <Link to="auth/register">Comenzar gratis</Link>
                        </Button>
                        <Button asChild variant="outline" size="lg" className="w-full sm:w-auto">
                            <Link to="auth/login">Ya tengo cuenta</Link>
                        </Button>
                    </div>

                    <p className="mt-3 text-xs text-muted-foreground">
                        Gratis y open source — sin tarjeta de crédito
                    </p>
                </div>
            </section>

            {/* How it works */}
            <section className="border-t bg-muted/30 px-4 py-16">
                <div className="mx-auto max-w-3xl">
                    <h2 className="mb-10 text-center text-xl font-heading font-semibold text-foreground md:text-2xl">
                        Comienza a estimar en 3 pasos
                    </h2>
                    <div className="grid gap-8 sm:grid-cols-3">
                        <FeatureStep
                            icon={<RiSendPlaneLine className="size-6" />}
                            step={1}
                            title="Crea una sesión"
                            description="Elige tu deck (Fibonacci, T-shirt) y comparte el enlace con tu equipo."
                        />
                        <FeatureStep
                            icon={<RiGroupLine className="size-6" />}
                            step={2}
                            title="Invita a tu equipo"
                            description="Comparte el código de sesión y todos se unen en segundos."
                        />
                        <FeatureStep
                            icon={<RiBarChartBoxLine className="size-6" />}
                            step={3}
                            title="¡Vota!"
                            description="Estimen en tiempo real, revelen las cartas y lleguen a consenso."
                        />
                    </div>
                </div>
            </section>

            {/* Footer */}
            <footer className="border-t px-4 py-6 text-center text-xs text-muted-foreground">
                Open<span className="font-semibold text-foreground">Poker</span> — Open Source Planning Poker
            </footer>
        </div>
    );
}