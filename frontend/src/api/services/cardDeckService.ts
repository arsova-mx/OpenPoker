import { instance } from "@/api/clients/APIClient";
import type { VotingDeckResponse, CardDeck, CardSeriesType } from "@/types";

export const MOCK_DECKS: CardDeck[] = [
  {
    id: "deck-fibonacci",
    name: "Fibonacci",
    seriesType: "FIBONACCI",
    values: ["0", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89", "?", "☕"],
    description: "Secuencia clásica ideal para estimaciones con incertidumbre creciente.",
  },
  {
    id: "deck-tshirt",
    name: "T-Shirt Sizes",
    seriesType: "T_SHIRT",
    values: ["XS", "S", "M", "L", "XL", "XXL", "?", "☕"],
    description: "Estimación relativa basada en tallas de ropa, ideal para roadmaps de alto nivel.",
  },
  {
    id: "deck-dot-voting",
    name: "Dot Voting",
    seriesType: "DOT_VOTING",
    values: ["1", "2", "3", "4", "5"],
    description: "Puntuación lineal rápida de 1 a 5 para consenso directo.",
  },
];

export const cardDeckService = {
  // GET /api/card-decks — Obtiene todas las series disponibles
  getAvailableDecks: async (): Promise<CardDeck[]> => {
    try {
      const response = await instance.get<VotingDeckResponse[]>("/card-decks");
      if (response.data && response.data.length > 0) {
        return response.data.map((deck) => ({
          id: deck.id,
          name: deck.name,
          seriesType: deck.seriesType as CardSeriesType,
          description: deck.description,
          values: deck.cards ? deck.cards.map((c) => c.value) : [],
          cards: deck.cards,
        }));
      }
      return MOCK_DECKS;
    } catch (error) {
      console.warn("Fallo al obtener barajas del backend, usando mocks:", error);
      return MOCK_DECKS;
    }
  },

  // GET /api/card-decks/{seriesType} — Obtiene una baraja por tipo
  getDeckBySeries: async (seriesType: CardSeriesType | string = "FIBONACCI"): Promise<VotingDeckResponse> => {
    try {
      const response = await instance.get<VotingDeckResponse>(`/card-decks/${seriesType}`);
      return response.data;
    } catch (error) {
      // Mock de respaldo por serie si el backend falla
      const mock = MOCK_DECKS.find((d) => d.seriesType === seriesType) || MOCK_DECKS[0];
      return {
        id: mock.id,
        name: mock.name,
        seriesType: mock.seriesType,
        description: mock.description,
        cards: mock.values.map((v, i) => ({ id: `mock-${v}`, value: v, orderIndex: i })),
      };
    }
  },
};