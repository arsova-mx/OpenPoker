import { useState } from "react";
import useAuthStore from "@/store/authStore";
import { useLogout } from "@/hooks/useLogout";
import SessionFormCreate from "./SessionFormCreate";
import SessionFormJoin from "./SessionFormJoin";
import HistoryModal from "../History/HistoryModal"; // 👈 Importamos el modal
import { Button } from "../ui/button";

export default function SessionLobby() {
  const username = useAuthStore((state) => state.username);
  const handleLogout = useLogout();
  const [isHistoryOpen, setIsHistoryOpen] = useState(false); // 👈 Estado del modal

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <div className="w-full max-w-md space-y-6 bg-card p-6 rounded-xl shadow-lg border border-border">
        {/* Cabecera */}
        <div className="text-center space-y-2">
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            Bienvenido, {username || "Usuario"}
          </h1>
          <p className="text-sm text-muted-foreground">
            Crea una nueva sala o únete a una existente
          </p>
        </div>

        {/* Botón de acceso rápido al Historial */}
        <Button
          variant="secondary"
          size="sm"
          className="w-full font-medium flex items-center justify-center gap-2"
          onClick={() => setIsHistoryOpen(true)}
        >
          🕒 Ver mi historial de sesiones y tickets
        </Button>

        {/* Sección: Crear Sesión */}
        <div className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
            Crear una sesión
          </h2>
          <SessionFormCreate />
        </div>

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <span className="w-full border-t border-border" />
          </div>
          <div className="relative flex justify-center text-xs uppercase">
            <span className="bg-card px-2 text-muted-foreground">O</span>
          </div>
        </div>

        {/* Sección: Unirse a Sesión */}
        <div className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
            Únete a una sesión
          </h2>
          <SessionFormJoin />
        </div>

        {/* Footer / Salir */}
        <div className="pt-4 border-t border-border">
          <Button
            variant="outline"
            size="sm"
            onClick={handleLogout}
            className="w-full"
          >
            Cerrar Sesión
          </Button>
        </div>
      </div>

      {/* Modal flotante */}
      <HistoryModal 
        isOpen={isHistoryOpen} 
        onClose={() => setIsHistoryOpen(false)} 
      />
    </div>
  );
}