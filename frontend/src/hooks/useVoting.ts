import { useState, useEffect, useCallback, useRef } from "react";
import axios from "axios";
import { voteService } from "@/api/services/voteService";
import { VoteResponse } from "@/types";

export const useVoting = (sessionCode: string, ticketId: string | null) => {
  const [selectedCard, setSelectedCard] = useState<string | null>(null);
  const [votes, setVotes] = useState<VoteResponse[]>([]);
  const [revealed, setRevealed] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Referencia para rastrear el ticket actual y descartar respuestas desfasadas
  const activeTicketRef = useRef<string | null>(ticketId);

  // 1. Limpiar estado de la mesa inmediatamente cuando cambia o se deselecciona el ticket
  useEffect(() => {
    activeTicketRef.current = ticketId;
    setSelectedCard(null);
    setVotes([]);
    setRevealed(false);
    setError(null);
  }, [ticketId]);

  // 2. Consultar los votos del ticket activo
  const fetchVotes = useCallback(async () => {
    if (!sessionCode || !ticketId) {
      setVotes([]);
      setRevealed(false);
      return;
    }

    try {
      const data = await voteService.getVotes(sessionCode, ticketId);

      // Si el ticket cambió mientras la petición viajaba por la red, se ignora
      if (activeTicketRef.current !== ticketId) return;

      setVotes(data.votes);
      setRevealed(data.revealed);
    } catch (err: unknown) {
      if (activeTicketRef.current !== ticketId) return;

      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || err.message || "Error al sincronizar votos");
      } else {
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
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || err.message || "Error al registrar el voto");
      } else {
        setError("Error al registrar el voto");
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
      setVotes(data.votes);
      setRevealed(data.revealed);
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || err.message || "Error al revelar votos");
      } else {
        setError("Error al revelar votos");
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