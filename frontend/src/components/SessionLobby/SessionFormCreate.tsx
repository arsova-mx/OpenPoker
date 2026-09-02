import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";

import { sessionServices } from "@/api/services/sessionServices";
import { Button } from "../ui/button";
import { Input } from "../ui/input";

export default function SessionFormCreate() {
  const navigate = useNavigate();

  const [sessionName, setSessionName] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleCreate = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!sessionName.trim()) {
      setErrorMessage("Por favor ingresa un nombre para la sesión");
      return;
    }

    try {
      setIsLoading(true);
      setErrorMessage(null);

      const response = await sessionServices.createSession({
        name: sessionName.trim(),
      });

      navigate(`/session/${response.sessionCode}`);
    } catch (error: unknown) {
      if (axios.isAxiosError(error)) {
        const message =
          error.response?.data?.message ||
          error.message ||
          "Error al crear la sesión";

        setErrorMessage(message);
      } else {
        setErrorMessage("Error inesperado al crear la sesión");
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleCreate} className="space-y-4">
      <div>
        <Input
          placeholder="Nombre para la sesión, ej. Sprint 32"
          value={sessionName}
          onChange={(e) => setSessionName(e.target.value)}
          disabled={isLoading}
        />

        {errorMessage && (
          <p className="mt-1 text-xs text-destructive">
            {errorMessage}
          </p>
        )}
      </div>

      <Button type="submit" disabled={isLoading} className="w-full">
        {isLoading ? "Creando sala..." : "Crear sala"}
      </Button>
    </form>
  );
}

