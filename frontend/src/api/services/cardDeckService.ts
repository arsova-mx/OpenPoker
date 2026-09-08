import { instance } from "@/api/clients/APIClient";
import type { VotingDeckResponse } from "@/types";

export const cardDeckService = {
  // GET /api/card-decks/FIBONACCI (o T_SHIRT, etc.)
  getDeckBySeries: async (seriesType: string = "FIBONACCI"): Promise<VotingDeckResponse> => {
    const response = await instance.get<VotingDeckResponse>(`/card-decks/${seriesType}`);
    return response.data;
  },
};