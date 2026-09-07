// frontend/src/api/services/voteService.ts
import { instance } from "@/api/clients/APIClient";
import { CastVoteRequest, VoteResponse, VotingResultsResponse } from "@/types";

export const voteService = {
  castVote: async (code: string, ticketId: string, data: CastVoteRequest): Promise<VoteResponse> => {
    const response = await instance.post<VoteResponse>(
      `/sessions/${code}/votes?ticketId=${ticketId}`,
      data
    );
    return response.data;
  },

  getVotes: async (code: string, ticketId: string): Promise<VotingResultsResponse> => {
    const response = await instance.get<VotingResultsResponse>(
      `/sessions/${code}/votes?ticketId=${ticketId}`,
      { params: { ticketId } }
    );
    return response.data;
  },

  revealVotes: async (code: string, ticketId: string): Promise<VotingResultsResponse> => {
    const response = await instance.post<VotingResultsResponse>(
      `/sessions/${code}/votes/reveal?ticketId=${ticketId}`
    );
    return response.data;
  },
};