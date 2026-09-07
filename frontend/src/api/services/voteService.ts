import { instance } from "@/api/clients/APIClient";
import {
  CastVoteRequest,
  VoteResponse,
  VotingResultsResponse,
} from "@/types";

export const voteService = {
  /**
   * Registra o actualiza el voto del usuario autenticado en la sesión.
   * Endpoint: POST http://localhost:8080/api/sessions/{code}/votes
   */
  castVote: async (code: string, data: CastVoteRequest): Promise<VoteResponse> => {
    const response = await instance.post<VoteResponse>(
      `/sessions/${code}/votes`,
      data
    );
    return response.data;
  },

  /**
   * Obtiene la lista de votos y el estado de la mesa (revelada o no).
   * Endpoint: GET http://localhost:8080/api/sessions/{code}/votes
   */
  getVotes: async (code: string): Promise<VotingResultsResponse> => {
    const response = await instance.get<VotingResultsResponse>(
      `/sessions/${code}/votes`
    );
    return response.data;
  },

  /**
   * Revela los votos de la sesión (acción reservada para el host).
   * Endpoint: POST http://localhost:8080/api/sessions/{code}/votes/reveal
   */
  revealVotes: async (code: string): Promise<VotingResultsResponse> => {
    const response = await instance.post<VotingResultsResponse>(
      `/sessions/${code}/votes/reveal`
    );
    return response.data;
  },
};

export default voteService;