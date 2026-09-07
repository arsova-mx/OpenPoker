import { useState, useEffect, useCallback, type FormEvent } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { sessionServices } from "@/api/services/sessionServices";
import { ticketService, type TicketResponse } from "@/api/services/ticketService";
import { useVoting } from "@/hooks/useVoting";
import useAuthStore from "@/store/authStore";
import CardDeck from "../CardDeck/CardDeck";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import type { SessionResponse } from "@/types";

export default function VotingBoard() {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const currentUsername = useAuthStore((state) => state.username);

  // Estados de sesión y tickets
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [tickets, setTickets] = useState<TicketResponse[]>([]);
  const [activeTicket, setActiveTicket] = useState<TicketResponse | null>(null);

  // Estado para crear nuevo ticket
  const [newTitle, setNewTitle] = useState("");
  const [isCreatingTicket, setIsCreatingTicket] = useState(false);

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

  // 1. Cargar detalles de sesión
  useEffect(() => {
    if (!code) return;
    sessionServices.getSession(code).then((data) => setSession(data));
  }, [code]);

  // 2. Cargar tickets de la sala con detección tolerante de campos
  const loadTickets = useCallback(async () => {
    if (!session?.id) return;
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const data: any[] = await ticketService.getBySession(session.id);
      setTickets(data);

      // Detecta tanto status como ticketStatus y title como tittle
      const current = data.find(
        (t) =>
          t.status === "VOTING" ||
          t.ticketStatus === "VOTING" ||
          t.status === "REVEALED" ||
          t.ticketStatus === "REVEALED"
      );

      if (current) {
        setActiveTicket({
          id: current.id,
          title: current.title || current.tittle || "Ticket sin título",
          gameSessionId: current.gameSessionId || session.id,
          status: current.status || current.ticketStatus,
        });
      }
    } catch {
      // Manejado globalmente por el interceptor de Axios
    }
  }, [session?.id]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  // 3. Crear nuevo ticket e iniciar votación de inmediato
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

      // Pasar a estado VOTING en base de datos
      await ticketService.updateStatus(created.id, "VOTING");

      // Transición directa en UI sin esperar round-trip de red
      setActiveTicket({
        id: created.id,
        title: created.title || created.tittle || newTitle.trim(),
        gameSessionId: session.id,
        status: "VOTING",
      });

      setNewTitle("");
      await loadTickets();
    } finally {
      setIsCreatingTicket(false);
    }
  };

  // 4. Iniciar votación de un ticket existente en WAITING
  const handleStartVoting = async (ticket: TicketResponse) => {
    await ticketService.updateStatus(ticket.id, "VOTING");
    setActiveTicket({
      ...ticket,
      status: "VOTING",
    });
    await loadTickets();
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
        <div className="flex gap-2">
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

      {/* Si NO hay ticket activo */}
      {!activeTicket && (
        <section className="w-full max-w-md my-auto bg-card border border-border p-6 rounded-xl shadow-sm text-center space-y-4">
          <h2 className="text-lg font-semibold">No hay ningún ticket en votación</h2>
          {isHost ? (
            <form onSubmit={handleCreateTicket} className="space-y-3">
              <p className="text-sm text-muted-foreground">
                Crea una historia para iniciar la estimación:
              </p>
              <Input
                placeholder="Título del ticket (ej. US-101 Login)"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                disabled={isCreatingTicket}
              />
              <Button type="submit" disabled={!newTitle.trim() || isCreatingTicket} className="w-full">
                {isCreatingTicket ? "Creando..." : "Crear e Iniciar Votación"}
              </Button>
            </form>
          ) : (
            <p className="text-sm text-muted-foreground">
              Esperando a que el host inicie la estimación de un ticket...
            </p>
          )}

          {/* Si hay tickets pendientes en WAITING creados previamente */}
          {isHost && tickets.length > 0 && (
            <div className="pt-4 border-t border-border text-left">
              <span className="text-xs font-semibold text-muted-foreground uppercase">
                Tickets en espera:
              </span>
              <ul className="mt-2 space-y-2">
                {tickets
                  // eslint-disable-next-line @typescript-eslint/no-explicit-any
                  .filter((t: any) => (t.status || t.ticketStatus) === "WAITING")
                  .map((t) => (
                    <li key={t.id} className="flex justify-between items-center bg-muted/40 p-2 rounded-md">
                      {/* eslint-disable-next-line @typescript-eslint/no-explicit-any */}
                      <span className="text-sm font-medium">{t.title || (t as any).tittle}</span>
                      <Button size="sm" variant="secondary" onClick={() => handleStartVoting(t)}>
                        Votar
                      </Button>
                    </li>
                  ))}
              </ul>
            </div>
          )}
        </section>
      )}

      {/* Si SÍ hay ticket activo */}
      {activeTicket && (
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
              Estimando:
            </span>
            <h2 className="text-xl font-bold text-foreground">{activeTicket.title}</h2>
          </div>

          {/* Participantes */}
          <section className="w-full max-w-4xl my-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-4 text-center">
              Participantes ({votes.length})
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

          {/* Baraja y botón de voto */}
          <section className="w-full max-w-4xl flex flex-col items-center gap-4 bg-card p-6 rounded-2xl border border-border shadow-sm">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
              Elige tu carta
            </h3>
            <CardDeck
              selectedCard={selectedCard}
              onSelectCard={setSelectedCard}
              disabled={votingLoading || revealed}
            />
            <Button
              onClick={castVote}
              disabled={!selectedCard || votingLoading || revealed}
              size="lg"
              className="w-full sm:w-64"
            >
              {votingLoading ? "Enviando..." : "Enviar voto"}
            </Button>
          </section>
        </>
      )}
    </main>
  );
}