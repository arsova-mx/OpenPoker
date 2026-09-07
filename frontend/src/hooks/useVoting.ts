// frontend/src/hooks/useVoting.ts
import { useState, useEffect, useCallback } from "react";
import axios from "axios";
import { voteService } from "@/api/services/voteService";
import { VoteResponse } from "@/types";

export const useVoting = (sessionCode: string, ticketId: string | null) => {
  const [selectedCard, setSelectedCard] = useState<string | null>(null);
  const [votes, setVotes] = useState<VoteResponse[]>([]);
  const [revealed, setRevealed] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Consulta los votos del ticket activo
  const fetchVotes = useCallback(async () => {
    if (!sessionCode || !ticketId) {
      setVotes([]);
      return;
    }
    try {
      const data = await voteService.getVotes(sessionCode, ticketId);
      setVotes(data.votes);
      setRevealed(data.revealed);
    } catch (err: unknown) {
      if (axios.isAxiosError(err)) {
        setError(err.response?.data?.message || err.message || "Error al sincronizar votos");
      } else {
        setError("Error al sincronizar votos");
      }
    }
  }, [sessionCode, ticketId]);

  // Polling cada 5 segundos SOLO si hay un ticket activo
  useEffect(() => {
    if (!ticketId) return;

    fetchVotes();
    const intervalId = setInterval(fetchVotes, 5000);

    return () => {
      clearInterval(intervalId);
    };
  }, [fetchVotes, ticketId]);

  // Enviar voto
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

  // Revelar votos
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