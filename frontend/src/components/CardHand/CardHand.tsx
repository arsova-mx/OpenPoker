import React from "react";
import type { CardSeriesType } from "@/types";

export interface CardHandItem {
  id?: string;
  value: string;
}

interface CardHandProps {
  /**
   * Puede recibir un array simple de strings (ej: ['1', '2', '3'])
   * o una lista de objetos con id (ej: CardValueResponse[])
   */
  values: string[] | CardHandItem[];
  selectedValue: string | null;
  seriesType?: CardSeriesType;
  disabled?: boolean;
  onSelectCard: (value: string, id?: string) => void;
}

export const CardHand: React.FC<CardHandProps> = ({
  values,
  selectedValue,
  seriesType = "FIBONACCI",
  disabled = false,
  onSelectCard,
}) => {
  // Normalizar los items para soportar tanto string[] como CardHandItem[]
  const normalizedCards: CardHandItem[] = values.map((item) =>
    typeof item === "string" ? { id: item, value: item } : item
  );

  // Estilos visuales según la serie
  const getCardStyle = (isSelected: boolean) => {
    const base =
      "relative select-none font-bold transition-all duration-200 flex items-center justify-center";

    switch (seriesType) {
      case "T_SHIRT":
        return `${base} h-20 w-16 rounded-xl border-2 text-lg ${
          isSelected
            ? "border-purple-500 bg-purple-600 text-white -translate-y-2 shadow-lg shadow-purple-500/30 ring-2 ring-purple-400"
            : "border-purple-900/40 bg-card text-purple-200 hover:-translate-y-1 hover:border-purple-400 hover:bg-purple-950/30"
        }`;

      case "DOT_VOTING":
        return `${base} h-16 w-16 rounded-full border-2 text-lg ${
          isSelected
            ? "border-amber-400 bg-amber-500 text-neutral-950 -translate-y-2 shadow-lg shadow-amber-500/30 ring-2 ring-amber-300"
            : "border-amber-900/40 bg-card text-amber-200 hover:-translate-y-1 hover:border-amber-400 hover:bg-amber-950/30"
        }`;

      case "FIBONACCI":
      default:
        return `${base} h-24 w-16 rounded-xl border-2 text-xl font-mono ${
          isSelected
            ? "border-primary bg-primary text-primary-foreground -translate-y-2 shadow-lg shadow-primary/30 ring-2 ring-primary ring-offset-2"
            : "border-border bg-card text-card-foreground hover:-translate-y-1 hover:border-primary/60 hover:shadow-md"
        }`;
    }
  };

  return (
    <div
      role="group"
      aria-label={`Baraja de estimación ${seriesType}`}
      className="flex flex-wrap items-center justify-center gap-3 p-4 bg-card/60 backdrop-blur-sm rounded-2xl border border-border shadow-sm w-full max-w-4xl"
    >
      {normalizedCards.map((card, idx) => {
        // Soporta comparación por ID o por Value directo
        const isSelected =
          selectedValue === card.id || selectedValue === card.value;

        // Renderizado especial de puntos para Dot Voting
        const numericVal = Number(card.value);
        const isDotEligible =
          seriesType === "DOT_VOTING" &&
          !isNaN(numericVal) &&
          numericVal >= 1 &&
          numericVal <= 5;

        return (
          <button
            key={card.id || `${card.value}-${idx}`}
            type="button"
            disabled={disabled}
            aria-pressed={isSelected}
            onClick={() => onSelectCard(card.value, card.id)}
            className={`
              ${getCardStyle(isSelected)}
              ${
                disabled
                  ? "cursor-not-allowed opacity-50 hover:translate-y-0 hover:shadow-none"
                  : "cursor-pointer"
              }
            `}
          >
            {isDotEligible ? (
              <div className="flex flex-col items-center justify-center">
                <span className="leading-none text-base font-bold">
                  {card.value}
                </span>
                <span className="text-xs leading-none tracking-widest text-current mt-0.5">
                  {"•".repeat(numericVal)}
                </span>
              </div>
            ) : (
              <span>{card.value}</span>
            )}
          </button>
        );
      })}
    </div>
  );
};

export default CardHand;