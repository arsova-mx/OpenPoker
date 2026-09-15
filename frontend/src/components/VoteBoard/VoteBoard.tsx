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
  // Mapa auxiliar para buscar el voto revelado por participante o username
  const votesByUsername = new Map<string, VoteResponse>(
    votes.map((v) => [v.username, v])
  );

  return (
    <section className="w-full max-w-4xl my-4">
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
            const pId = participant.id || participant.participantId || "";
            const name = participant.effectiveName || participant.displayName || participant.username || "Anónimo";
            const isCurrentUser = name === currentUsername;
            
            // Saber si ya votó: consultamos el status map o si ya existe en la lista de votos
            const hasVoted = Boolean(voteStatusMap[pId] || votesByUsername.has(name));
            const voteData = votesByUsername.get(name);

            return (
              <article
                key={pId || name}
                className={`p-4 rounded-xl border flex flex-col items-center justify-center gap-2 shadow-sm transition-colors ${
                  isCurrentUser ? "border-primary/50 bg-primary/5" : "border-border bg-card"
                }`}
              >
                <span className="font-semibold text-sm truncate max-w-[120px]">
                  {name} {isCurrentUser && "(Tú)"}
                </span>

                <div className="h-14 w-10 rounded-md border border-border bg-muted flex items-center justify-center font-bold text-lg">
                  {revealed ? (
                    <span>{voteData ? voteData.cardValue : "—"}</span>
                  ) : hasVoted ? (
                    <span className="text-green-600 dark:text-green-400" title="Voto emitido">✅</span>
                  ) : (
                    <span className="text-muted-foreground" title="Pensando...">⏳</span>
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