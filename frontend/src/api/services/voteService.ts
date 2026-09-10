import { instance } from "@/api/clients/APIClient";
import type { CastVoteRequest, VoteResponse, VotingResultsResponse } from "@/types";

export const voteService = {
  castVote: async (code: string, ticketId: string, data: CastVoteRequest): Promise<VoteResponse> => {
    const response = await instance.post<VoteResponse>(
      `/sessions/${code}/votes`,
      data,
      { params: { ticketId } }
    );
    return response.data;
  },

  getVotes: async (code: string, ticketId: string): Promise<VotingResultsResponse> => {
    const response = await instance.get<VotingResultsResponse>(
      `/sessions/${code}/votes`,
      { params: { ticketId } }
    );
    return response.data;
  },

  revealVotes: async (code: string, ticketId: string): Promise<VotingResultsResponse> => {
    const response = await instance.post<VotingResultsResponse>(
      `/sessions/${code}/votes/reveal`,
      null,
      { params: { ticketId } }
    );
    return response.data;
  },
};