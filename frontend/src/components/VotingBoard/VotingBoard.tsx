import { useParams, useNavigate } from "react-router-dom";
import { useVoting } from "@/hooks/useVoting";
import useAuthStore from "@/store/authStore";
import CardDeck from "../CardDeck/CardDeck";
import { Button } from "../ui/button";

export default function VotingBoard() {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const currentUsername = useAuthStore((state) => state.username);

  const {
    selectedCard,
    setSelectedCard,
    castVote,
    votes,
    revealed,
    revealVotes,
    loading,
    error,
  } = useVoting(code || "");

  return (
    <main className="min-h-screen bg-background p-6 flex flex-col items-center justify-between">
      {/* Cabecera de la sala */}
      <header className="w-full max-w-4xl flex items-center justify-between border-b border-border pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Mesa de Votación</h1>
          <p className="text-sm text-muted-foreground">
            Código de sala: <span className="font-mono font-bold text-primary">{code}</span>
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" size="sm" onClick={() => navigate("/home")}>
            Volver al lobby
          </Button>
          <Button
            variant="default"
            size="sm"
            onClick={revealVotes}
            disabled={loading || revealed}
          >
            {revealed ? "Votos Revelados" : "Revelar votos"}
          </Button>
        </div>
      </header>

      {/* Alertas de error */}
      {error && (
        <section aria-label="Errores" className="w-full max-w-4xl mt-4 p-3 bg-destructive/15 border border-destructive rounded-lg text-sm text-destructive text-center">
          {error}
        </section>
      )}

      {/* Mesa central: Lista de Participantes */}
      <section className="w-full max-w-4xl my-8">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-4 text-center">
          Participantes ({votes.length})
        </h2>

        {votes.length === 0 ? (
          <p className="text-center text-sm text-muted-foreground py-8">
            Esperando a que los participantes voten...
          </p>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            {votes.map((vote) => {
              const isCurrentUser = vote.username === currentUsername;
              return (
                <article
                  key={vote.voteId}
                  className={`p-4 rounded-xl border flex flex-col items-center justify-center gap-2 shadow-sm transition-all
                    ${isCurrentUser ? "border-primary/50 bg-primary/5" : "border-border bg-card"}
                  `}
                >
                  <span className="font-semibold text-sm truncate max-w-[120px]">
                    {vote.username} {isCurrentUser && "(Tú)"}
                  </span>
                  <div className="h-14 w-10 rounded-md border border-border bg-muted flex items-center justify-center font-bold text-lg">
                    {revealed ? (
                      <span>{vote.cardValue}</span>
                    ) : (
                      <span className="text-primary">{vote.cardValue ? "✓" : "..."}</span>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {/* Selector de Cartas y Botón de Voto */}
      <section className="w-full max-w-4xl flex flex-col items-center gap-4 bg-card p-6 rounded-2xl border border-border shadow-sm">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          Elige tu carta
        </h2>

        <CardDeck
          selectedCard={selectedCard}
          onSelectCard={setSelectedCard}
          disabled={loading || revealed}
        />

        <Button
          onClick={castVote}
          disabled={!selectedCard || loading || revealed}
          size="lg"
          className="w-full sm:w-64"
        >
          {loading ? "Enviando..." : "Enviar voto"}
        </Button>
      </section>
    </main>
  );
}