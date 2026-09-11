import { useState, useEffect, useCallback, useRef } from "react";
import axios from "axios";
import { voteService } from "@/api/services/voteService";
import type { VoteResponse } from "@/types";

export const useVoting = (sessionCode: string, ticketId: string | null) => {
  const [selectedCard, setSelectedCard] = useState<string | null>(null);
  const [votes, setVotes] = useState<VoteResponse[]>([]);
  const [revealed, setRevealed] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Rastrea el ticket actual para descartar respuestas de tickets previos
  const activeTicketRef = useRef<string | null>(ticketId);

  // 1. Limpiar mesa de inmediato al cambiar o deseleccionar ticket
  useEffect(() => {
    activeTicketRef.current = ticketId;
    setSelectedCard(null);
    setVotes([]);
    setRevealed(false);
    setError(null);
  }, [ticketId]);

  // 2. Consultar votos del ticket activo
  const fetchVotes = useCallback(async () => {
    if (!sessionCode || !ticketId) {
      setVotes([]);
      setRevealed(false);
      return;
    }

    try {
      const data = await voteService.getVotes(sessionCode, ticketId);

      // Si el ticket cambió mientras la petición viajaba, se ignora la respuesta
      if (activeTicketRef.current !== ticketId) return;

      setVotes(data?.votes ?? []);
      setRevealed(data?.revealed ?? false);
      setError(null);
    } catch (err: unknown) {
      // Si el usuario cambió de ticket durante la petición, se ignoran 404 y cualquier otro error
      if (activeTicketRef.current !== ticketId) return;

      if (axios.isAxiosError(err) && err.response?.status === 404) {
        // Ticket nuevo sin votos inicializados aún
        setVotes([]);
        setRevealed(false);
        setError(null);
      } else if (axios.isAxiosError(err)) {
        // Errores HTTP reales (400, 403, 500, etc.)
        setError(err.response?.data?.message || err.message || "Error al sincronizar votos");
      } else {
        // Errores de red o inesperados
        setError("Error al sincronizar votos");
      }
    }
  }, [sessionCode, ticketId]);

  // 3. Polling cada 5 segundos únicamente cuando existe un ticket seleccionado
  useEffect(() => {
    if (!ticketId) return;

    fetchVotes();
    const intervalId = setInterval(fetchVotes, 5000);

    return () => {
      clearInterval(intervalId);
    };
  }, [fetchVotes, ticketId]);

  // 4. Enviar voto
  const castVote = async () => {
    if (!selectedCard || !sessionCode || !ticketId) return;
    setLoading(true);
    setError(null);
    try {
      await voteService.castVote(sessionCode, ticketId, { cardValue: selectedCard });
      await fetchVotes();
    } catch (err: unknown) {
      if (activeTicketRef.current === ticketId) {
        if (axios.isAxiosError(err)) {
          setError(err.response?.data?.message || err.message || "Error al registrar el voto");
        } else {
          setError("Error al registrar el voto");
        }
      }
    } finally {
      setLoading(false);
    }
  };

  // 5. Revelar votos
  const revealVotes = async () => {
    if (!sessionCode || !ticketId) return;
    setLoading(true);
    setError(null);
    try {
      const data = await voteService.revealVotes(sessionCode, ticketId);
      if (activeTicketRef.current === ticketId) {
        setVotes(data.votes);
        setRevealed(data.revealed);
      }
    } catch (err: unknown) {
      if (activeTicketRef.current === ticketId) {
        if (axios.isAxiosError(err)) {
          setError(err.response?.data?.message || err.message || "Error al revelar votos");
        } else {
          setError("Error al revelar votos");
        }
      }
    } finally {
      setLoading(false);
    }
  };

  return {
    selectedCard,
    setSelectedCard,
    castVote,
    votes,
    revealed,
    revealVotes,
    loading,
    error,
  };
};

export default useVoting;