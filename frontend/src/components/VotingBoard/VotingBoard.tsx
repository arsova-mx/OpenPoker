import { useState, useEffect, useCallback, type FormEvent, type MouseEvent } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { sessionServices } from "@/api/services/sessionServices";
import { ticketService, type TicketResponse, type TicketStatus } from "@/api/services/ticketService";
import { cardDeckService } from "@/api/services/cardDeckService";
import { useVoting } from "@/hooks/useVoting";
import { useStompClient } from "@/hooks/useStompClient";
import useAuthStore from "@/store/authStore";
import CardDeck from "../CardDeck/CardDeck";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import type { SessionResponse, CardValueResponse, Participant } from "@/types";

export default function VotingBoard() {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const rawUsername = useAuthStore((state) => state.username);
  const token = useAuthStore((state) => state.token);

  // Fallback seguro: si el store aún no hidrata el username, consulta localStorage o usa un alias legible
  const currentUsername = rawUsername || localStorage.getItem("username") || "Participante";

  // Estados de sesión, baraja y tickets
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [deckCards, setDeckCards] = useState<CardValueResponse[]>([]);
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [activeTicket, setActiveTicket] = useState<TicketResponse | null>(null);

  // Estado reactivo de participantes vía WebSocket
  const [participants, setParticipants] = useState<Participant[]>([]);

  // Conexión STOMP nativa
  const { connected: wsConnected, subscribe, publish } = useStompClient(token);

  // Estado para crear nuevo ticket y toggle del formulario
  const [newTitle, setNewTitle] = useState("");
  const [isCreatingTicket, setIsCreatingTicket] = useState(false);
  const [showCreateForm, setShowCreateForm] = useState(false);

  // Hook de votación enlazado al ticket activo
  const {
    selectedCard,
    setSelectedCard,
    castVote,
    votes,
    revealed,
    revealVotes,
    loading: votingLoading,
    error: votingError,
  } = useVoting(code || "", activeTicket ? activeTicket.id : null);

  const isHost = session?.hostUsername === currentUsername;

  // 1. Cargar detalles de sesión y baraja
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

  // 2. Conexión WebSocket STOMP: Join seguro y suscripción a participantes
  useEffect(() => {
    // Validamos que haya conexión, código y un nombre definido antes de emitir
    if (!wsConnected || !code || !currentUsername) return;

    publish("/app/session.join", {
      inviteCode: code,
      username: currentUsername,
    });

    const sub = subscribe(`/topic/session/${code}/participants`, (data: Participant[]) => {
      if (Array.isArray(data)) {
        setParticipants(data);
      }
    });

    return () => {
      sub?.unsubscribe();
    };
  }, [wsConnected, code, currentUsername, publish, subscribe]);

  // 3. Cargar tickets de la sala directamente tipados y normalizados
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
        if (!serverTicket) return prev;

        if (prev.status === "VOTING" && serverTicket.status === "WAITING") {
          return { ...serverTicket, status: "VOTING" };
        }

        return serverTicket;
      });
    } catch {
      // Manejado por interceptor global
    }
  }, [session?.id]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  // 4. Cambiar estado de cualquier ticket y actualizar UI inmediatamente
  const handleChangeTicketStatus = async (
    e: MouseEvent,
    ticketId: string,
    newStatus: TicketStatus
  ) => {
    e.stopPropagation();
    try {
      await ticketService.updateStatus(ticketId, newStatus);

      setActiveTicket((prev) => {
        if (!prev || prev.id === ticketId) {
          const target = tickets.find((item) => item.id === ticketId);
          return target ? { ...target, status: newStatus } : null;
        }
        if (newStatus === "VOTING") {
          const target = tickets.find((item) => item.id === ticketId);
          return target ? { ...target, status: "VOTING" } : prev;
        }
        return prev;
      });

      setTickets((prev) =>
        prev.map((t) => (t.id === ticketId ? { ...t, status: newStatus } : t))
      );

      await loadTickets();
    } catch (err) {
      console.error("Error al actualizar estatus del ticket:", err);
    }
  };

  // 5. Seleccionar un ticket para ver sus detalles en mesa
  const handleSelectTicket = (ticket: TicketResponse) => {
    setActiveTicket(ticket);
    setShowCreateForm(false);
  };

  // 6. Crear ticket nuevo
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

      setActiveTicket({
        ...created,
        status: "VOTING",
      });

      setNewTitle("");
      setShowCreateForm(false);
      await loadTickets();
    } finally {
      setIsCreatingTicket(false);
    }
  };

  const totalParticipants = participants.length > 0 ? participants.length : (session?.participantCount ?? 1);

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
              className={`inline-block h-2 w-2 rounded-full ${
                wsConnected ? "bg-green-500" : "bg-red-500"
              }`}
              title={wsConnected ? "WebSocket Conectado" : "Desconectado"}
            />
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
            <Button
              variant="default"
              size="sm"
              onClick={revealVotes}
              disabled={votingLoading || revealed}
            >
              {revealed ? "Votos Revelados" : "Revelar votos"}
            </Button>
          )}
        </div>
      </header>

      {/* Panel en vivo de Participantes en la Sala */}
      <section className="w-full max-w-4xl my-3 p-3 bg-card border border-border rounded-xl shadow-sm">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            Conectados en vivo ({totalParticipants})
          </span>
        </div>
        <div className="flex flex-wrap gap-2">
          {participants.length > 0 ? (
            participants.map((p, index) => {
              const participantName = p.username ?? p.displayName ?? "Participante";
              const isMe = participantName === currentUsername;
              const itemKey = p.id || `${participantName}-${index}`;

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
              {currentUsername || "Conectando participantes..."}
            </span>
          )}
        </div>
      </section>

      {/* Formulario desplegable para crear tickets */}
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

      {/* Backlog de tickets con controles de estado */}
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
            {tickets.map((t) => {
              const isCurrent = activeTicket?.id === t.id;

              return (
                <li key={t.id} className="py-2.5 flex flex-wrap items-center justify-between gap-2">
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

                  {/* Acciones de Host para cambiar estatus */}
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

          {/* Participantes y votos emitidos */}
          <section className="w-full max-w-4xl my-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-4 text-center">
              Votos emitidos: {votes.length} de {totalParticipants}
            </h3>
            {votes.length === 0 ? (
              <p className="text-center text-sm text-muted-foreground py-6">
                Esperando a que los participantes emitan su voto...
              </p>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                {votes.map((vote) => {
                  const isCurrentUser = vote.username === currentUsername;
                  return (
                    <article
                      key={vote.voteId}
                      className={`p-4 rounded-xl border flex flex-col items-center justify-center gap-2 shadow-sm
                        ${isCurrentUser ? "border-primary/50 bg-primary/5" : "border-border bg-card"}
                      `}
                    >
                      <span className="font-semibold text-sm truncate max-w-[120px]">
                        {vote.username} {isCurrentUser && "(Tú)"}
                      </span>
                      <div className="h-14 w-10 rounded-md border border-border bg-muted flex items-center justify-center font-bold text-lg">
                        {revealed ? <span>{vote.cardValue}</span> : <span className="text-primary">✓</span>}
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>

          {/* Baraja */}
          <section className="w-full max-w-4xl flex flex-col items-center gap-4 bg-card p-6 rounded-2xl border border-border shadow-sm">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Elige tu carta
            </h3>
            <CardDeck
              cards={deckCards}
              selectedCardId={selectedCard}
              onSelectCard={setSelectedCard}
              disabled={votingLoading || revealed || activeTicket.status !== "VOTING"}
            />
            <Button
              onClick={castVote}
              disabled={!selectedCard || votingLoading || revealed || activeTicket.status !== "VOTING"}
              size="lg"
              className="w-full sm:w-64"
            >
              {activeTicket.status !== "VOTING"
                ? `Ticket en estado ${activeTicket.status}`
                : votingLoading
                ? "Enviando..."
                : "Enviar voto"}
            </Button>
          </section>
        </>
      ) : (
        <section className="text-center my-12 text-muted-foreground">
          <p>Selecciona un ticket del listado arriba o crea uno nuevo para comenzar la votación.</p>
        </section>
      )}
    </main>
  );
}