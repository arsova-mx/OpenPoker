import { Link, Outlet, redirect } from "react-router-dom";
import { RiCheckboxCircleLine } from "@remixicon/react";

const features = [
    "Estimaciones en tiempo real con tu equipo",
    "Decks Fibonacci, T-shirt y personalizados",
    "Sesiones con código para unirse al instante",
    "Resultados visuales y consenso rápido",
    "100% open source y gratuito",
];

export default function Auth() {
    return (
        <div className="flex min-h-dvh">
            {/* Left panel — product pitch (hidden on mobile) */}
            <div className="hidden w-1/2 flex-col justify-between bg-primary p-10 text-primary-foreground lg:flex">
                <Link to="/" className="text-xl font-heading font-bold">
                    Open<span className="opacity-80">Poker</span>
                </Link>

                <div className="max-w-md">
                    <h2 className="text-3xl font-heading font-bold leading-tight">
                        La forma más fácil de estimar con tu equipo
                    </h2>
                    <p className="mt-3 text-sm leading-relaxed opacity-80">
                        Planning Poker open source para equipos ágiles que buscan velocidad, consenso y diversión.
                    </p>
                    <ul className="mt-8 flex flex-col gap-3">
                        {features.map((feature) => (
                            <li key={feature} className="flex items-center gap-2 text-sm">
                                <RiCheckboxCircleLine className="size-5 shrink-0 opacity-80" />
                                {feature}
                            </li>
                        ))}
                    </ul>
                </div>

                <p className="text-xs opacity-60">
                    © {new Date().getFullYear()} OpenPoker — Open Source Planning Poker
                </p>
            </div>

            {/* Right panel — form */}
            <div className="flex w-full flex-col items-center justify-center px-4 py-10 lg:w-1/2 lg:px-12">
                {/* Mobile brand (visible only on small screens) */}
                <Link to="/" className="mb-8 text-2xl font-heading font-bold text-foreground lg:hidden">
                    Open<span className="text-primary">Poker</span>
                </Link>
                <div className="w-full max-w-md">
                    <Outlet />
                </div>
            </div>
        </div>
    )
}

export async function action() {
    
    return redirect("/");
}