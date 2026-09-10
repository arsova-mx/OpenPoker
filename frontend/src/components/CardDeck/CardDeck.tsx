export interface CardOption {
  id: string;
  value: string;
}

interface CardDeckProps {
  cards: CardOption[];
  selectedCardId: string | null;
  onSelectCard: (cardId: string) => void;
  disabled?: boolean;
}

export const CardDeck = ({
  cards,
  selectedCardId,
  onSelectCard,
  disabled = false,
}: CardDeckProps) => {
  return (
    <div
      className="flex flex-wrap items-center justify-center gap-3 p-4"
      role="group"
      aria-label="Baraja de cartas"
    >
      {cards.map((card) => {
        const isSelected = selectedCardId === card.id;

        return (
          <button
            key={card.id}
            type="button"
            disabled={disabled}
            onClick={() => onSelectCard(card.id)}
            aria-pressed={isSelected}
            className={`
              relative flex h-24 w-16 select-none items-center justify-center rounded-xl border-2 font-bold text-xl transition-all duration-200
              ${
                isSelected
                  ? "border-primary bg-primary text-primary-foreground -translate-y-2 shadow-lg shadow-primary/30 ring-2 ring-primary ring-offset-2"
                  : "border-border bg-card text-card-foreground hover:-translate-y-1 hover:border-primary/60 hover:shadow-md"
              }
              ${disabled ? "cursor-not-allowed opacity-50 hover:translate-y-0 hover:shadow-none" : "cursor-pointer"}
            `}
          >
            <span>{card.value}</span>
          </button>
        );
      })}
    </div>
  );
};

export default CardDeck;