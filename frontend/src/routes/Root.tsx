
import { Outlet } from "react-router-dom";
import { Toaster } from "@/components/ui/sonner";

export default function Root() {

    return (
        <div className="min-h-dvh bg-background">
            <Outlet />
            <Toaster richColors closeButton />
        </div>
    );
}
