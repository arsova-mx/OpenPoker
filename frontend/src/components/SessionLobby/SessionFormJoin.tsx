import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";

import { sessionServices } from "@/api/services/sessionServices";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

export default function SessionFormJoin() {
  const navigate = useNavigate();

  const [sessionCode, setSessionCode] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleJoin = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const cleanCode = sessionCode.trim().toUpperCase();

    // 1. Error inline exclusivo para validación local síncrona
    if (!/^[A-Z0-9]{6}$/.test(cleanCode)) {
      setErrorMessage(
        "El código debe tener exactamente 6 caracteres alfanuméricos"
      );
      return;
    }

    try {
      setIsLoading(true);
      setErrorMessage(null);

      await sessionServices.joinSession(cleanCode);

      navigate(`/session/${cleanCode}`);
    } catch {
      // 2. El interceptor de APIClient ya dispara el toast global.
      // No seteamos errorMessage para evitar duplicar avisos en pantalla.
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleJoin} className="space-y-4">
      <div className="space-y-1">
        {/* Label accesible asociado al input */}
        <label
          htmlFor="session-code-input"
          className="text-sm font-medium text-foreground"
        >
          Código de la sala
        </label>

        <Input
          id="session-code-input"
          placeholder="Código de 6 caracteres, ej. 7B9WII"
          value={sessionCode}
          onChange={(e) => {
            setSessionCode(e.target.value.toUpperCase());
            if (errorMessage) setErrorMessage(null);
          }}
          maxLength={6}
          disabled={isLoading}
          aria-invalid={!!errorMessage}
          aria-describedby={errorMessage ? "session-code-error" : undefined}
          className="text-center font-mono uppercase tracking-widest"
        />

        {errorMessage && (
          <p
            id="session-code-error"
            role="alert"
            className="mt-1 text-center text-xs text-destructive"
          >
            {errorMessage}
          </p>
        )}
      </div>

      <Button type="submit" disabled={isLoading} className="w-full">
        {isLoading ? "Uniéndose..." : "Unirse a sala"}
      </Button>
    </form>
  );
} 
