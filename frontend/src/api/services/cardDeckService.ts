import { instance } from "@/api/clients/APIClient";
import type { VotingDeckResponse, CardDeckType } from "@/types";

export const cardDeckService = {
  // GET /api/card-decks/FIBONACCI (o T_SHIRT)
  getDeckBySeries: async (seriesType: CardDeckType = "FIBONACCI"): Promise<VotingDeckResponse> => {
    const response = await instance.get<VotingDeckResponse>(`/card-decks/${seriesType}`);
    return response.data;
  },
};