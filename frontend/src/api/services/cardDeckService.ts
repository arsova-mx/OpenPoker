import { instance } from "@/api/clients/APIClient";
import type { VotingDeckResponse, CardDeck, CardSeriesType } from "@/types";

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
      return [];
    } catch (error) {
      console.warn("Fallo al obtener barajas del backend:", error);
      // Retornar arreglo vacío para evitar IDs no-UUID y permitir fallback a POST /sessions
      return [];
    }
  },

  // GET /api/card-decks/{seriesType} — Obtiene una baraja por tipo
  getDeckBySeries: async (
    seriesType: CardSeriesType | string = "FIBONACCI"
  ): Promise<VotingDeckResponse> => {
    // No atrapar el error con IDs falsos: dejar que rechace si falla
    const response = await instance.get<VotingDeckResponse>(`/card-decks/${seriesType}`);
    return response.data;
  },
};