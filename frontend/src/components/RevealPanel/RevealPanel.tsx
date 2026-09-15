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
  if (!statistics) return null;

  return (
    <section className="w-full max-w-4xl my-4 p-5 bg-card border border-border rounded-2xl shadow-sm text-center">
      <h3 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-4">
        Resultados de la Votación
      </h3>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Promedio */}
        <div className="p-3 bg-muted/40 rounded-xl border border-border">
          <span className="text-xs text-muted-foreground block">Promedio</span>
          <span className="text-2xl font-bold text-foreground">
            {typeof statistics.average === "number" ? statistics.average.toFixed(1) : "—"}
          </span>
        </div>

        {/* Consenso */}
        <div
          className={`p-3 rounded-xl border transition-colors ${
            statistics.isFullConsensus
              ? "bg-green-500/10 border-green-500/40 text-green-700 dark:text-green-400"
              : "bg-muted/40 border-border"
          }`}
        >
          <span className="text-xs text-muted-foreground block">Consenso</span>
          <span className="text-2xl font-bold">
            {statistics.isFullConsensus ? "¡100%!" : `${Math.round(statistics.consensusPercentage)}%`}
          </span>
        </div>

        {/* Carta Sugerida */}
        <div className="p-3 bg-muted/40 rounded-xl border border-border">
          <span className="text-xs text-muted-foreground block">Carta Recomendada</span>
          <span className="text-2xl font-bold text-primary">
            {suggestedCardValue || "—"}
          </span>
        </div>
      </div>
    </section>
  );
};

export default RevealPanel;