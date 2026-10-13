import { instance } from "@/api/clients/APIClient";
import { TicketResponse } from "@/api/services/ticketService";

export interface GitHubIssue {
  id: number;
  number: number;
  title: string;
  body?: string;
  html_url: string;
  state: string;
}

export interface QueryGitHubIssuesRequest {
  repo: string;
  personalAccessToken?: string;
}

export interface ImportGitHubRequest {
  sessionId: string;
  repo: string;
  personalAccessToken?: string;
  issues: GitHubIssue[];
}

export interface ExportEstimationResponse {
  message: string;
}

export const githubService = {
  // POST /api/integrations/github/issues/query
  fetchIssues: async (repo: string, token?: string): Promise<GitHubIssue[]> => {
    const payload: QueryGitHubIssuesRequest = {
      repo,
      personalAccessToken: token || undefined,
    };
    const response = await instance.post<GitHubIssue[]>("/integrations/github/issues/query", payload);
    return response.data;
  },

  // POST /api/integrations/github/import
  importIssues: async (data: ImportGitHubRequest): Promise<TicketResponse[]> => {
    const response = await instance.post<TicketResponse[]>("/integrations/github/import", data);
    return response.data;
  },

  // POST /api/integrations/github/tickets/{ticketId}/export
  exportEstimation: async (
    ticketId: string, 
    token: string, 
    customComment?: string
  ): Promise<ExportEstimationResponse> => {
    const response = await instance.post<ExportEstimationResponse>(
      `/integrations/github/tickets/${ticketId}/export`, 
      {
        personalAccessToken: token,
        customComment: customComment || undefined,
      }
    );
    return response.data;
  },
};