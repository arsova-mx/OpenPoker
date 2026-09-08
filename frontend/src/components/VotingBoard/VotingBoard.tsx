import { useState, useEffect, useCallback, type FormEvent } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { sessionServices } from "@/api/services/sessionServices";
import { ticketService, type TicketResponse } from "@/api/services/ticketService";
import { cardDeckService } from "@/api/services/cardDeckService";
import { useVoting } from "@/hooks/useVoting";
import useAuthStore from "@/store/authStore";
import CardDeck from "../CardDeck/CardDeck";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import type { SessionResponse, CardValueResponse } from "@/types";

export default function VotingBoard() {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const currentUsername = useAuthStore((state) => state.username);

  // Estados de sesión, baraja y tickets
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [deckCards, setDeckCards] = useState<CardValueResponse[]>([]);
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [activeTicket, setActiveTicket] = useState<TicketResponse | null>(null);

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

  // 2. Cargar tickets de la sala
  const loadTickets = useCallback(async () => {
    if (!session?.id) return;
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const data: any[] = await ticketService.getBySession(session.id);
      setTickets(data);

      setActiveTicket((prev) => {
        if (!prev) {
          const current = data.find(
            (t) =>
              t.status === "VOTING" ||
              t.ticketStatus === "VOTING" ||
              t.status === "REVEALED" ||
              t.ticketStatus === "REVEALED"
          );
          return current ? {
            id: current.id,
            title: current.title || current.tittle || "Ticket",
            gameSessionId: current.gameSessionId || session.id,
            status: current.status || current.ticketStatus,
          } : null;
        }

        // Buscar el ticket actualizado en la lista
        const serverTicket = data.find((t) => t.id === prev.id);
        if (!serverTicket) return prev;

        const serverStatus = serverTicket.status || serverTicket.ticketStatus;

        return {
          id: serverTicket.id,
          title: serverTicket.title || serverTicket.tittle || prev.title,
          gameSessionId: serverTicket.gameSessionId || session.id,
          status: serverStatus || prev.status,
        };
      });
    } catch {
      // Manejado por interceptor
    }
  }, [session?.id]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  // 3. Cambiar estado de cualquier ticket y actualizar UI inmediatamente
  const handleChangeTicketStatus = async (
    e: React.MouseEvent,
    ticketId: string,
    newStatus: "WAITING" | "VOTING" | "FINISHED" | "REVEALED"
  ) => {
    e.stopPropagation();
    try {
      await ticketService.updateStatus(ticketId, newStatus);

      // Actualizar estado activo en tiempo real
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

      // Actualizar lista local de inmediato
      setTickets((prev) =>
        prev.map((t) => (t.id === ticketId ? { ...t, status: newStatus } : t))
      );

      await loadTickets();
    } catch (err) {
      console.error("Error al actualizar estatus del ticket:", err);
    }
  };

  // 4. Seleccionar un ticket para ver sus detalles en mesa
  const handleSelectTicket = (ticket: TicketResponse) => {
    setActiveTicket(ticket);
    setShowCreateForm(false);
  };

  // 5. Crear ticket nuevo
  const handleCreateTicket = async (e: FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim() || !session?.id) return;

    setIsCreatingTicket(true);
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const created: any = await ticketService.createTicket({
        title: newTitle.trim(),
        gameSessionId: session.id,
      });

      await ticketService.updateStatus(created.id, "VOTING");

      setActiveTicket({
        id: created.id,
        title: created.title || created.tittle || newTitle.trim(),
        gameSessionId: session.id,
        status: "VOTING",
      });

      setNewTitle("");
      setShowCreateForm(false);
      await loadTickets();
    } finally {
      setIsCreatingTicket(false);
    }
  };

  return (
    <main className="min-h-screen bg-background p-6 flex flex-col items-center justify-between">
      {/* Cabecera */}
      <header className="w-full max-w-4xl flex items-center justify-between border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">
            {session?.name || "Mesa de Votación"}
          </h1>
          <p className="text-sm text-muted-foreground">
            Código: <span className="font-mono font-bold text-primary">{code}</span>
            {isHost && (
              <span className="ml-2 text-xs bg-primary/10 text-primary px-2 py-0.5 rounded-full font-semibold">
                Host
              </span>
            )}
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

      {/* Formulario desplegable para crear más tickets */}
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
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              const status = t.status || (t as any).ticketStatus;

              return (
                <li key={t.id} className="py-2.5 flex flex-wrap items-center justify-between gap-2">
                  <div
                    className="flex items-center gap-3 cursor-pointer hover:opacity-80"
                    onClick={() => handleSelectTicket(t)}
                  >
                    <span
                      className={`h-2.5 w-2.5 rounded-full ${
                        status === "VOTING"
                          ? "bg-amber-500 animate-pulse"
                          : status === "REVEALED" || status === "FINISHED"
                          ? "bg-green-500"
                          : "bg-muted-foreground"
                      }`}
                    />
                    {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
                    <span className={`text-sm ${isCurrent ? "font-bold text-primary underline" : "text-foreground"}`}>
                      {t.title || (t as any).tittle}
                    </span>
                    <span className="text-xs font-mono bg-muted px-1.5 py-0.5 rounded text-muted-foreground">
                      {status}
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

                      {status !== "VOTING" && status !== "FINISHED" && (
                        <Button
                          size="sm"
                          variant="secondary"
                          className="h-8 text-xs bg-amber-500/10 text-amber-600 hover:bg-amber-500/20"
                          onClick={(e) => handleChangeTicketStatus(e, t.id, "VOTING")}
                        >
                          Activar Votación
                        </Button>
                      )}

                      {status === "VOTING" && (
                        <Button
                          size="sm"
                          variant="outline"
                          className="h-8 text-xs"
                          onClick={(e) => handleChangeTicketStatus(e, t.id, "WAITING")}
                        >
                          Pausar
                        </Button>
                      )}

                      {status !== "FINISHED" && (
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

          {/* Participantes */}
          <section className="w-full max-w-4xl my-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-4 text-center">
              Participantes en la sala: {session?.participantCount ?? 1} (Votos: {votes.length})
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