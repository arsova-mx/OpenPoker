import React from "react";
import type { VoteStatistics } from "@/types";

interface RevealPanelProps {
  statistics?: VoteStatistics | null;
  suggestedCardValue?: string | null;
  totalVotes: number;
}

export const RevealPanel: React.FC<RevealPanelProps> = ({
  statistics,
  suggestedCardValue,
  totalVotes,
}) => {
  const avg = statistics?.average != null ? statistics.average.toFixed(1) : "—";
  const consensus = statistics?.consensusPercentage != null 
    ? `${Math.round(statistics.consensusPercentage)}%` 
    : "—";
  const isFull = Boolean(statistics?.isFullConsensus);

  return (
    <section className="w-full max-w-4xl my-4 p-5 bg-card border border-border rounded-2xl shadow-sm text-center">
      <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-4">
        Resultados de la Votación ({totalVotes} {totalVotes === 1 ? "voto" : "votos"})
      </h3>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Promedio */}
        <div className="p-3 bg-muted/40 rounded-xl border border-border">
          <span className="text-xs text-muted-foreground block">Promedio</span>
          <span className="text-2xl font-bold text-foreground">{avg}</span>
        </div>

        {/* Consenso */}
        <div
          className={`p-3 rounded-xl border transition-colors ${
            isFull
              ? "bg-green-500/10 border-green-500/40 text-green-700 dark:text-green-400"
              : "bg-muted/40 border-border"
          }`}
        >
          <span className="text-xs text-muted-foreground block">Consenso</span>
          <span className="text-2xl font-bold">{isFull ? "¡100%!" : consensus}</span>
        </div>

        {/* Carta Sugerida */}
        <div className="p-3 bg-muted/40 rounded-xl border border-border">
          <span className="text-xs text-muted-foreground block">Carta Recomendada</span>
          <span className="text-2xl font-bold text-primary">
            {suggestedCardValue && suggestedCardValue !== "N/A" ? suggestedCardValue : "—"}
          </span>
        </div>
      </div>
    </section>
  );
};

export default RevealPanel;