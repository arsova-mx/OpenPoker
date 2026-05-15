import { useEffect } from "react";
import { connectToSession } from "@/services/websocket";

export default function SessionPage() {

  useEffect(() => {
    const stomp = connectToSession("session-id", "ABC123", "Juan");

    return () => {
      if (stomp) {
        stomp.disconnect(); // 🔌 cerrar conexión al salir
      }
    };

  }, []);

  return (
    <div>
      <h1>Sesión</h1>
    </div>
  );
}