import React, { useEffect, useState } from "react";
import type { CardDeck } from "@/types";
import { cardDeckService } from "@/api/services/cardDeckService";

interface SeriesSelectorProps {
  selectedDeckId?: string | null;
  onSelectDeck: (deck: CardDeck) => void;
  disabled?: boolean;
}

export const SeriesSelector: React.FC<SeriesSelectorProps> = ({
  selectedDeckId,
  onSelectDeck,
  disabled = false,
}) => {
  const [decks, setDecks] = useState<CardDeck[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    cardDeckService.getAvailableDecks().then((availableDecks) => {
      if (!isMounted) return;
      setDecks(availableDecks);
      setLoading(false);

      // Si no hay ninguno seleccionado previamente, seleccionar Fibonacci por defecto
      if (!selectedDeckId && availableDecks.length > 0) {
        const defaultDeck =
          availableDecks.find((d) => d.seriesType === "FIBONACCI") || availableDecks[0];
        onSelectDeck(defaultDeck);
      }
    });

    return () => {
      isMounted = false;
    };
  }, []);

  if (loading) {
    return (
      <div className="py-2 text-xs text-muted-foreground animate-pulse">
        Cargando series de estimación...
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-foreground block">
        Serie de estimación
      </label>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        {decks.map((deck) => {
          const isSelected = selectedDeckId === deck.id;

          return (
            <div
              key={deck.id}
              role="button"
              tabIndex={0}
              aria-pressed={isSelected}
              onClick={() => !disabled && onSelectDeck(deck)}
              onKeyDown={(e) => {
                if (!disabled && (e.key === "Enter" || e.key === " ")) {
                  e.preventDefault();
                  onSelectDeck(deck);
                }
              }}
              className={`p-3.5 rounded-xl border text-left transition-all duration-150 flex flex-col justify-between select-none ${
                disabled ? "opacity-60 cursor-not-allowed" : "cursor-pointer"
              } ${
                isSelected
                  ? "border-primary bg-primary/10 shadow-sm ring-1 ring-primary/40"
                  : "border-border bg-card hover:border-primary/50 hover:bg-accent/40"
              }`}
            >
              <div>
                <div className="flex items-center justify-between mb-1">
                  <span className="font-semibold text-sm text-foreground">
                    {deck.name}
                  </span>
                  {isSelected && (
                    <span className="h-2 w-2 rounded-full bg-primary inline-block" />
                  )}
                </div>

                <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
                  {deck.description}
                </p>
              </div>

              {/* Preview de las primeras cartas */}
              <div className="flex flex-wrap gap-1 mt-3 pt-2 border-t border-border/50">
                {deck.values.slice(0, 7).map((val, idx) => (
                  <span
                    key={`${deck.id}-preview-${idx}`}
                    className="px-1.5 py-0.5 text-[10px] rounded bg-muted text-muted-foreground font-mono font-medium"
                  >
                    {val}
                  </span>
                ))}
                {deck.values.length > 7 && (
                  <span className="text-[10px] text-muted-foreground self-center ml-0.5">
                    +{deck.values.length - 7}
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default SeriesSelector;