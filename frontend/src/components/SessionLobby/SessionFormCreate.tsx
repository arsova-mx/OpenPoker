import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";

import { sessionServices } from "@/api/services/sessionServices";
import { SeriesSelector } from "@/components/SeriesSelector/SeriesSelector"; // O "@/components/SeriesSelector" si no usaste subcarpeta
import type { CardDeck } from "@/types";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

export default function SessionFormCreate() {
  const navigate = useNavigate();

  const [sessionName, setSessionName] = useState("");
  const [selectedDeck, setSelectedDeck] = useState<CardDeck | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleCreate = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // 1. Error inline exclusivo para validación local síncrona
    if (!sessionName.trim()) {
      setErrorMessage("Por favor ingresa un nombre para la sesión");
      return;
    }

    try {
      setIsLoading(true);
      setErrorMessage(null);

      // 2. Enviamos el deckId seleccionado hacia el backend (si es undefined, usará el deck por defecto)
      const response = await sessionServices.createSession(
        {
          name: sessionName.trim(),
        },
        selectedDeck?.id
      );

      navigate(`/session/${response.sessionCode}`);
    } catch {
      // 3. El interceptor de APIClient ya dispara el toast global.
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleCreate} className="space-y-5">
      <div className="space-y-1">
        <label
          htmlFor="session-name-input"
          className="text-sm font-medium text-foreground"
        >
          Nombre de la sesión
        </label>

        <Input
          id="session-name-input"
          placeholder="Nombre para la sesión, ej. Sprint 32"
          value={sessionName}
          onChange={(e) => {
            setSessionName(e.target.value);
            if (errorMessage) setErrorMessage(null);
          }}
          disabled={isLoading}
          aria-invalid={!!errorMessage}
          aria-describedby={errorMessage ? "session-name-error" : undefined}
        />

        {errorMessage && (
          <p
            id="session-name-error"
            role="alert"
            className="mt-1 text-xs text-destructive"
          >
            {errorMessage}
          </p>
        )}
      </div>

      {/* Selector de series obtenido desde la base de datos */}
      <SeriesSelector
        selectedDeckId={selectedDeck?.id}
        onSelectDeck={setSelectedDeck}
        disabled={isLoading}
      />

      <Button type="submit" disabled={isLoading} className="w-full">
        {isLoading ? "Creando sala..." : "Crear sala"}
      </Button>
    </form>
  );
}