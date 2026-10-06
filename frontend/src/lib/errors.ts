import axios from "axios";

/**
 * Obtiene un mensaje legible de un error de la API.
 * El backend puede responder un string plano o un objeto JSON (errores de Spring, ProblemDetail, etc.).
 */
export function getErrorMessage(error: unknown, fallback = "Ocurrió un error inesperado"): string {
  const data: unknown = axios.isAxiosError(error) ? error.response?.data : error;

  if (typeof data === "string" && data.trim()) {
    return data;
  }

  if (data && typeof data === "object") {
    const body = data as Record<string, unknown>;
    for (const key of ["message", "detail", "error", "title"]) {
      const value = body[key];
      if (typeof value === "string" && value.trim()) {
        return value;
      }
    }
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}
