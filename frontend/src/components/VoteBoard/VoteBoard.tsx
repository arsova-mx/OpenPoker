import React from "react";
import type { Participant, VoteResponse, VoteStatusMap } from "@/types";

interface VoteBoardProps {
  participants: Participant[];
  votes: VoteResponse[];
  voteStatusMap: VoteStatusMap;
  revealed: boolean;
  currentUsername: string;
}

export const VoteBoard: React.FC<VoteBoardProps> = ({
  participants,
  votes,
  voteStatusMap,
  revealed,
  currentUsername,
}) => {
  // Indexación por username para búsqueda O(1) de votos revelados
  const votesByUsername = new Map<string, VoteResponse>(
    votes.map((v) => [v.username, v])
  );

  return (
    <section className="w-full max-w-4xl my-4" aria-label="Tablero de votación de participantes">
      <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground mb-4 text-center">
        Participantes ({participants.length})
      </h3>

      {participants.length === 0 ? (
        <p className="text-center text-sm text-muted-foreground py-6">
          Esperando a que los participantes se unan...
        </p>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
          {participants.map((participant) => {
            const pId = participant.id || "";
            const altId = participant.participantId || "";
            const name =
              participant.effectiveName ||
              participant.displayName ||
              participant.username ||
              "Anónimo";
            const isCurrentUser = name === currentUsername;

            // Validación exhaustiva contra IDs, alias y registro local
            const hasVoted = Boolean(
              (pId && voteStatusMap[pId]) ||
              (altId && voteStatusMap[altId]) ||
              (name && voteStatusMap[name]) ||
              votesByUsername.has(name) ||
              (isCurrentUser && voteStatusMap[currentUsername])
            );

            const voteData = votesByUsername.get(name);

            // Texto descriptivo accesible para lectores de pantalla
            const accessibleStatusText = revealed
              ? voteData
                ? `${name} Votó ${voteData.cardValue}`
                : `${name} no tiene voto registrado`
              : hasVoted
              ? `${name} ya emitió su voto`
              : `${name} está pensando su voto`;

            return (
              <article
                key={pId || altId || name}
                className={`p-4 rounded-xl border flex flex-col items-center justify-center gap-2 shadow-sm transition-colors ${
                  isCurrentUser ? "border-primary/50 bg-primary/5" : "border-border bg-card"
                }`}
              >
                <span className="font-semibold text-sm truncate max-w-[120px]">
                  {name} {isCurrentUser && "(Tú)"}
                </span>

                {/* Región viva que anuncia el cambio de estado de manera no intrusiva */}
                <div
                  role="status"
                  aria-live="polite"
                  className="h-14 w-10 rounded-md border border-border bg-muted flex items-center justify-center font-bold text-lg relative"
                >
                  {/* Texto explícito oculto visualmente, exclusivo para tecnologías de asistencia */}
                  <span className="sr-only">{accessibleStatusText}</span>

                  {/* Representación visual decorativa ignorada por el lector de pantalla */}
                  {revealed ? (
                    <span aria-hidden="true">{voteData ? voteData.cardValue : "—"}</span>
                  ) : hasVoted ? (
                    <span aria-hidden="true" className="text-green-600 dark:text-green-400">
                      ✅
                    </span>
                  ) : (
                    <span aria-hidden="true" className="text-muted-foreground">
                      ⏳
                    </span>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
};

export default VoteBoard;