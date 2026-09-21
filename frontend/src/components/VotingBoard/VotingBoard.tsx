import { useState, useEffect, useCallback, useMemo, type FormEvent, type MouseEvent } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { sessionServices } from "@/api/services/sessionServices";
import { ticketService, type TicketResponse, type TicketStatus } from "@/api/services/ticketService";
import { cardDeckService } from "@/api/services/cardDeckService";
import { useVoting } from "@/hooks/useVoting";
import { useStompClient } from "@/hooks/useStompClient";
import useAuthStore from "@/store/authStore";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import CardSelector from "../CardSelector/CardSelector";
import VoteBoard from "../VoteBoard/VoteBoard";
import RevealPanel from "../RevealPanel/RevealPanel";
import type { 
  SessionResponse, 
  CardValueResponse, 
  Participant, 
  VoteStatusMap, 
  VotingRRAverageResponse 
} from "@/types";

// Extensión defensiva en caso de que el backend envíe ticketId en el payload
interface ExtendedVotingRRAverageResponse extends VotingRRAverageResponse {
  ticketId?: string;
}

export default function VotingBoard() {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();

  // 1. Token y usuario unificados
  const storeToken = useAuthStore((state) => state.token);
  const effectiveToken = useMemo(() => {
    return storeToken || localStorage.getItem("token");
  }, [storeToken]);

  const rawUsername = useAuthStore((state) => state.username);
  const currentUsername = rawUsername || localStorage.getItem("username") || "Participante";
  const isGuest = !effectiveToken;

  // Estados de sesión, baraja y tickets
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [deckCards, setDeckCards] = useState<CardValueResponse[]>([]);
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [activeTicket, setActiveTicket] = useState<TicketResponse | null>(null);

  // Estados reactivos WebSocket
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [voteStatusMap, setVoteStatusMap] = useState<VoteStatusMap>({});
  const [sessionVotesData, setSessionVotesData] = useState<VotingRRAverageResponse | null>(null);

  // Bloqueo de concurrencia para evitar envíos múltiples
  const [isRevealing, setIsRevealing] = useState(false);

  // Cliente STOMP
  const { connected: wsConnected, subscribe, publish } = useStompClient(effectiveToken);

  // Formularios de tickets
  const [newTitle, setNewTitle] = useState("");
  const [isCreatingTicket, setIsCreatingTicket] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);

  // Hook de votación REST
  const {
    selectedCard,
    setSelectedCard,
    castVote,
    votes,
    revealed,
    revealVotes,
    resetVotes,
    loading: votingLoading,
    error: votingError,
  } = useVoting(code || "", activeTicket ? activeTicket.id : null);

  const isHost = session?.hostUsername === currentUsername;

  // Cargar sesión y baraja inicial
  useEffect(() => {
    if (!code) return;
    sessionServices.getSession(code).then((data) => setSession(data));

    cardDeckService
      .getDeckBySeries("FIBONACCI")
      .then((deck) => {
        if (deck?.cards) {
          setDeckCards(deck.cards);
        }
      })
      .catch(() => {});
  }, [code]);

  // Suscripciones STOMP y Join seguro
  useEffect(() => {
    if (!wsConnected || !code || !currentUsername) return;

    // 1. Suscripción a Participantes
    const subParticipants = subscribe<Participant[]>(`/topic/session/${code}/participants`, (data) => {
      if (Array.isArray(data)) {
        setParticipants(data);
      }
    });

    // 2. Suscripción a Quién votó
    const subVoteStatus = subscribe<VoteStatusMap>(`/topic/session/${code}/vote-status`, (data) => {
      if (data && typeof data === "object") {
        setVoteStatusMap(data);
      }
    });

    // 3. Suscripción a Resultados de Votación (Filtrada por ticketId)
    const subVotes = subscribe<ExtendedVotingRRAverageResponse>(`/topic/session/${code}/votes`, (data) => {
      if (data && typeof data === "object") {
        // Ignorar si el mensaje trae ticketId y no coincide con el ticket activo
        if (data.ticketId && activeTicket && data.ticketId !== activeTicket.id) {
          return;
        }

        if (data.revealed) {
          setSessionVotesData(data);
          setActiveTicket((prev) => (prev ? { ...prev, status: "REVEALED" } : null));
          setTickets((prev) =>
            prev.map((t) => {
              if (data.ticketId) {
                return t.id === data.ticketId ? { ...t, status: "REVEALED" } : t;
              }
              return t.id === activeTicket?.id ? { ...t, status: "REVEALED" } : t;
            })
          );
        } else {
          setSessionVotesData(null);
          setSelectedCard(null);
          setVoteStatusMap({});
          setActiveTicket((prev) => (prev ? { ...prev, status: "VOTING" } : null));
          setTickets((prev) =>
            prev.map((t) => {
              if (data.ticketId) {
                return t.id === data.ticketId ? { ...t, status: "VOTING" } : t;
              }
              return t.id === activeTicket?.id ? { ...t, status: "VOTING" } : t;
            })
          );
        }
      }
    });

    // 4. Suscripción a Tickets actualizados o reseteados en tiempo real
    const subTicketUpdated = subscribe<TicketResponse>(
      `/topic/session/${code}/ticket-updated`,
      (updatedTicket) => {
        if (updatedTicket?.id) {
          setTickets((prev) => {
            const exists = prev.some((t) => t.id === updatedTicket.id);
            if (exists) {
              return prev.map((t) => (t.id === updatedTicket.id ? updatedTicket : t));
            }
            return [updatedTicket, ...prev];
          });

          setActiveTicket((prev) => {
            if (!prev || prev.id === updatedTicket.id) {
              if (updatedTicket.status === "VOTING") {
                setSessionVotesData(null);
                setSelectedCard(null);
                setVoteStatusMap({});
              }
              return updatedTicket;
            }
            return prev;
          });
        }
      }
    );

    // 5. Emitir Join seguro
    const joinPayload: Record<string, string> = { inviteCode: code };
    if (isGuest) {
      joinPayload.guestName = currentUsername;
    } else {
      joinPayload.username = currentUsername;
    }
    if (activeTicket?.id) {
      joinPayload.ticketId = activeTicket.id;
    }
    publish("/app/session.join", joinPayload);

    return () => {
      subParticipants?.unsubscribe();
      subVoteStatus?.unsubscribe();
      subVotes?.unsubscribe();
      subTicketUpdated?.unsubscribe();
    };
  }, [wsConnected, code, currentUsername, isGuest, publish, subscribe, setSelectedCard, activeTicket]);

  // Limpiar estados de votación cuando cambia el ticket activo
  useEffect(() => {
    setVoteStatusMap({});
    setSessionVotesData(null);
  }, [activeTicket?.id]);

  // Carga inicial de tickets
  const loadTickets = useCallback(async () => {
    if (!session?.id) return;
    try {
      const data = await ticketService.getBySession(session.id);
      setTickets(data);

      setActiveTicket((prev) => {
        if (!prev) {
          const current = data.find((t) => t.status === "VOTING" || t.status === "REVEALED");
          return current || null;
        }
        const serverTicket = data.find((t) => t.id === prev.id);
        return serverTicket || prev;
      });
    } catch {
      // Manejado por interceptor global
    }
  }, [session?.id]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  // Cambiar estado manual del ticket
  const handleChangeTicketStatus = async (
    e: MouseEvent,
    ticketId: string,
    newStatus: TicketStatus
  ) => {
    e.stopPropagation();
    try {
      const updated = await ticketService.updateStatus(ticketId, newStatus);

      // Si se activa votación o si ya era el ticket activo, se sincroniza en la mesa
      if (newStatus === "VOTING" || activeTicket?.id === ticketId) {
        setActiveTicket(updated);
      }
      setTickets((prev) => prev.map((t) => (t.id === ticketId ? updated : t)));

      if (wsConnected && code) {
        publish("/app/session.ticket-update", {
          inviteCode: code,
          ticket: updated,
        });
      }

      await loadTickets();
    } catch (err) {
      console.error("Error al actualizar estatus del ticket:", err);
    }
  };

  const handleSelectTicket = (ticket: TicketResponse) => {
    setActiveTicket(ticket);
    setShowCreateForm(false);
  };

  const handleCreateTicket = async (e: FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim() || !session?.id) return;

    setIsCreatingTicket(true);
    try {
      const created = await ticketService.createTicket({
        title: newTitle.trim(),
        gameSessionId: session.id,
      });

      await ticketService.updateStatus(created.id, "VOTING");

      const activeCreated: TicketResponse = {
        ...created,
        status: "VOTING",
      };

      setActiveTicket(activeCreated);
      setTickets((prev) => [activeCreated, ...prev]);

      if (wsConnected && code) {
        publish("/app/session.ticket-update", {
          inviteCode: code,
          ticket: activeCreated,
        });
      }

      setNewTitle("");
      setShowCreateForm(false);
      await loadTickets();
    } finally {
      setIsCreatingTicket(false);
    }
  };

  // Voto: Envío por WebSocket delegado a /vote-status para confirmación real
  const handleSubmitVote = async () => {
    if (!selectedCard || !code || !activeTicket) return;

    try {
      if (wsConnected) {
        // En STOMP no actualizamos voteStatusMap optimísticamente;
        // el servidor confirma emitiendo en /topic/session/${code}/vote-status.
        publish("/app/session.vote", {
          cardValue: selectedCard,
          ticketId: activeTicket.id,
        });
      } else {
        // En fallback HTTP sí sabemos si la petición completó con éxito
        await castVote();

        const me = participants.find(
          (p) => (p.effectiveName || p.username || p.displayName) === currentUsername
        );
        const myId = me?.id || me?.participantId || currentUsername;
        setVoteStatusMap((prev) => ({
          ...prev,
          [myId]: true,
          [currentUsername]: true,
        }));
      }
    } catch (err) {
      console.error("Error al emitir el voto:", err);
    }
  };

  // Revelar: WebSocket con fallback a REST
  const handleRevealVotes = async () => {
    if (!activeTicket || isRevealing) return;
    setIsRevealing(true);

    try {
      if (wsConnected && code) {
        publish("/app/session.reveal", {
          ticketId: activeTicket.id,
        });
      } else {
        await revealVotes();
      }
    } finally {
      setTimeout(() => setIsRevealing(false), 1500);
    }
  };

  // Nueva Ronda: Backend primero (WS o REST reset) antes de alterar la UI local
  const handleResetVotes = async () => {
    if (!activeTicket || !session || !code) return;

    setIsRevealing(false);

    try {
      if (wsConnected) {
        publish("/app/session.reset-votes", {
          ticketId: activeTicket.id,
        });
      } else {
        // Fallback REST real que borra votos en base de datos
        await resetVotes();
      }

      // Solo actualizar estado si la solicitud completó con éxito
      setSelectedCard(null);
      setVoteStatusMap({});
      setSessionVotesData(null);

      const resetTicket: TicketResponse = {
        ...activeTicket,
        status: "VOTING",
      };
      setActiveTicket(resetTicket);
      setTickets((prev) => prev.map((t) => (t.id === activeTicket.id ? resetTicket : t)));
    } catch (error) {
      console.error("No se pudo reiniciar la ronda en el servidor:", error);
    }
  };

  // Estado de revelación unificado y reactivo
  const isRevealed = Boolean(
    sessionVotesData?.revealed ||
    activeTicket?.status === "REVEALED" ||
    revealed
  );

  const displayVotes = isRevealed
    ? (sessionVotesData?.votes ?? votes)
    : votes;

  return (
    <main className="min-h-screen bg-background p-6 flex flex-col items-center justify-between">
      {/* Cabecera */}
      <header className="w-full max-w-4xl flex items-center justify-between border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {session?.name || "Mesa de Votación"}
          </h1>
          <p className="text-sm text-muted-foreground flex items-center gap-2">
            <span>
              Código: <strong className="font-mono text-primary">{code}</strong>
            </span>
            {isHost && (
              <span className="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded-full font-semibold">
                Host
              </span>
            )}
            <span
              role="status"
              aria-label={wsConnected ? "WebSocket conectado" : "WebSocket desconectado"}
              className={`inline-block h-2 w-2 rounded-full ${
                wsConnected ? "bg-green-500" : "bg-red-500"
              }`}
              title={wsConnected ? "WebSocket Conectado" : "Desconectado"}
            >
              <span className="sr-only">
                {wsConnected ? "WebSocket conectado" : "WebSocket desconectado"}
              </span>
            </span>
          </p>
        </div>
        <div className="flex items-center gap-2">
          {isHost && (
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setShowCreateForm(!showCreateForm)}
            >
              {showCreateForm ? "Cerrar creador" : "+ Crear ticket"}
            </Button>
          )}
          <Button variant="outline" size="sm" onClick={() => navigate("/home")}>
            Volver al lobby
          </Button>
          
          {isHost && activeTicket && (
            <>
              {!isRevealed ? (
                <Button
                  variant="default"
                  size="sm"
                  onClick={handleRevealVotes}
                  disabled={votingLoading || isRevealing}
                >
                  {isRevealing ? "Revelando..." : "Revelar votos"}
                </Button>
              ) : (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={handleResetVotes}
                >
                  Nueva ronda
                </Button>
              )}
            </>
          )}
        </div>
      </header>

      {/* Participantes en vivo */}
      <section className="w-full max-w-4xl my-3 p-3 bg-card border border-border rounded-xl shadow-sm">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Conectados en vivo ({participants.length})
          </span>
        </div>
        <div className="flex flex-wrap gap-2">
          {participants.length > 0 ? (
            participants.map((p, index) => {
              const participantName = p.effectiveName ?? p.username ?? p.displayName ?? "Participante";
              const isMe = participantName === currentUsername;
              const itemKey = p.id || p.participantId || `${participantName}-${index}`;

              return (
                <span
                  key={itemKey}
                  className={`text-xs px-2.5 py-1 rounded-full border flex items-center gap-1.5 font-medium ${
                    isMe
                      ? "bg-primary/10 border-primary text-primary"
                      : "bg-muted text-muted-foreground border-border"
                  }`}
                >
                  <span className="h-1.5 w-1.5 rounded-full bg-green-500 inline-block" />
                  {participantName} {isMe && "(Tú)"}
                  {p.role === "HOST" && (
                    <span className="text-[10px] uppercase font-bold text-amber-500">★</span>
                  )}
                </span>
              );
            })
          ) : (
            <span className="text-xs text-muted-foreground">
              Conectando participantes...
            </span>
          )}
        </div>
      </section>

      {/* Formulario de tickets */}
      {showCreateForm && isHost && (
        <section className="w-full max-w-4xl my-4 p-4 bg-muted/40 border border-border rounded-xl">
          <h2 className="text-sm font-semibold mb-2">Crear nuevo ticket en esta sala</h2>
          <form onSubmit={handleCreateTicket} className="flex gap-2">
            <Input
              placeholder="Título del ticket (ej. US-204 Notificaciones)"
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              disabled={isCreatingTicket}
              className="bg-background"
            />
            <Button type="submit" disabled={!newTitle.trim() || isCreatingTicket}>
              {isCreatingTicket ? "Creando..." : "Crear e Iniciar"}
            </Button>
          </form>
        </section>
      )}

      {/* Backlog */}
      <section className="w-full max-w-4xl my-4 bg-card p-4 rounded-xl border border-border shadow-sm">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
            Tickets en la Sala ({tickets.length})
          </h2>
          {activeTicket && (
            <span className="text-xs bg-primary/10 text-primary px-2 py-1 rounded-md font-medium">
              Viendo en mesa: {activeTicket.title}
            </span>
          )}
        </div>

        {tickets.length === 0 ? (
          <p className="text-sm text-muted-foreground">No hay tickets registrados aún.</p>
        ) : (
          <ul className="divide-y divide-border">
            {tickets.map((t, index) => {
              const isCurrent = activeTicket?.id === t.id;

              return (
                <li key={`${t.id}-${index}`} className="py-2.5 flex flex-wrap items-center justify-between gap-2">
                  <div
                    className="flex items-center gap-3 cursor-pointer hover:opacity-80"
                    onClick={() => handleSelectTicket(t)}
                  >
                    <span
                      className={`h-2.5 w-2.5 rounded-full ${
                        t.status === "VOTING"
                          ? "bg-amber-500 animate-pulse"
                          : t.status === "REVEALED" || t.status === "FINISHED"
                          ? "bg-green-500"
                          : "bg-muted-foreground"
                      }`}
                    />
                    <span className={`text-sm ${isCurrent ? "font-bold text-primary underline" : "text-foreground"}`}>
                      {t.title}
                    </span>
                    <span className="text-xs font-mono bg-muted px-1.5 py-0.5 rounded text-muted-foreground">
                      {t.status}
                    </span>
                  </div>

                  {isHost && (
                    <div className="flex items-center gap-1.5" onClick={(e) => e.stopPropagation()}>
                      <Button
                        size="sm"
                        variant={isCurrent ? "default" : "outline"}
                        className="h-8 text-xs"
                        onClick={() => handleSelectTicket(t)}
                      >
                        {isCurrent ? "En mesa" : "Ver detalles"}
                      </Button>

                      {t.status !== "VOTING" && t.status !== "FINISHED" && (
                        <Button
                          size="sm"
                          variant="secondary"
                          className="h-8 text-xs bg-amber-500/10 text-amber-600 hover:bg-amber-500/20"
                          onClick={(e) => handleChangeTicketStatus(e, t.id, "VOTING")}
                        >
                          Activar Votación
                        </Button>
                      )}

                      {t.status === "VOTING" && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-8 text-xs"
                          onClick={(e) => handleChangeTicketStatus(e, t.id, "WAITING")}
                        >
                          Pausar
                        </Button>
                      )}

                      {t.status !== "FINISHED" && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-8 text-xs text-muted-foreground hover:text-destructive"
                          onClick={(e) => handleChangeTicketStatus(e, t.id, "FINISHED")}
                        >
                          Finalizar
                        </Button>
                      )}
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {/* Mesa central de votación */}
      {activeTicket ? (
        <>
          {votingError && (
            <section
              aria-label="Errores"
              className="w-full max-w-4xl mt-4 p-3 bg-destructive/15 border border-destructive rounded-lg text-sm text-destructive text-center"
            >
              {votingError}
            </section>
          )}

          <div className="text-center my-4">
            <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Estimando ({activeTicket.status}):
            </span>
            <h2 className="text-xl font-bold text-foreground">{activeTicket.title}</h2>
          </div>

          {/* Panel de estadísticas cuando la ronda se revela */}
          {isRevealed && (
            <RevealPanel
              statistics={sessionVotesData?.statistics}
              suggestedCardValue={sessionVotesData?.suggestedCardValue}
              totalVotes={displayVotes.length}
            />
          )}

          {/* Tablero de participantes con cartas / checks */}
          <VoteBoard
            participants={participants}
            votes={displayVotes}
            voteStatusMap={voteStatusMap}
            revealed={isRevealed}
            currentUsername={currentUsername}
          />

          {/* Selector de baraja */}
          <CardSelector
            cards={deckCards}
            selectedCardId={selectedCard}
            onSelectCard={setSelectedCard}
            onSubmitVote={handleSubmitVote}
            disabled={votingLoading || isRevealed || activeTicket.status !== "VOTING"}
            loading={votingLoading}
          />
        </>
      ) : (
        <section className="text-center my-12 text-muted-foreground">
          <p>Selecciona un ticket del listado arriba o crea uno nuevo para comenzar la votación.</p>
        </section>
      )}
    </main>
  );
}