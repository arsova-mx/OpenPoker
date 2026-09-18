import React from "react";
import type { CardValueResponse } from "@/types";
import { Button } from "@/components/ui/button";

interface CardSelectorProps {
  cards: CardValueResponse[];
  selectedCardId: string | null;
  onSelectCard: (cardId: string) => void;
  onSubmitVote: () => void;
  disabled?: boolean;
  loading?: boolean;
}

export const CardSelector: React.FC<CardSelectorProps> = ({
  cards,
  selectedCardId,
  onSelectCard,
  onSubmitVote,
  disabled = false,
  loading = false,
}) => {
  return (
    <section className="w-full max-w-4xl flex flex-col items-center gap-4 bg-card p-6 rounded-2xl border border-border shadow-sm">
      <h3 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        Elige tu carta
      </h3>

      <div
        className="flex flex-wrap items-center justify-center gap-3 p-2"
        role="group"
        aria-label="Baraja de estimación"
      >
        {cards.map((card) => {
          const isSelected = selectedCardId === card.id;
          return (
            <button
              key={card.id}
              type="button"
              disabled={disabled || loading}
              onClick={() => onSelectCard(card.id)}
              aria-pressed={isSelected}
              className={`
                relative flex h-24 w-16 select-none items-center justify-center rounded-xl border-2 font-bold text-xl transition-all duration-200
                ${
                  isSelected
                    ? "border-primary bg-primary text-primary-foreground -translate-y-2 shadow-lg shadow-primary/30 ring-2 ring-primary ring-offset-2"
                    : "border-border bg-card text-card-foreground hover:-translate-y-1 hover:border-primary/60 hover:shadow-md"
                }
                ${disabled || loading ? "cursor-not-allowed opacity-50 hover:translate-y-0 hover:shadow-none" : "cursor-pointer"}
              `}
            >
              <span>{card.value}</span>
            </button>
          );
        })}
      </div>

      <Button
        onClick={onSubmitVote}
        disabled={!selectedCardId || disabled || loading}
        size="lg"
        className="w-full sm:w-64"
      >
        {loading ? "Enviando voto..." : "Enviar voto"}
      </Button>
    </section>
  );
};

export default CardSelector;