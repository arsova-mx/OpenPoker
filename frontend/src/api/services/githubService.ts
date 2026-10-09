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

export interface ImportGitHubRequest {
  sessionId: string;
  repo: string;
  personalAccessToken?: string;
  issues: GitHubIssue[];
}

export const githubService = {
  // GET /api/integrations/github/issues?repo=owner/repo&token=...
  fetchIssues: async (repo: string, token?: string): Promise<GitHubIssue[]> => {
    const response = await instance.get<GitHubIssue[]>("/integrations/github/issues", {
      params: { repo, token: token || undefined },
    });
    return response.data;
  },

  // POST /api/integrations/github/import
  importIssues: async (data: ImportGitHubRequest): Promise<TicketResponse[]> => {
    const response = await instance.post<TicketResponse[]>("/integrations/github/import", data);
    return response.data;
  },

  // POST /api/integrations/github/tickets/{ticketId}/export
  exportEstimation: async (ticketId: string, token: string, customComment?: string) => {
    const response = await instance.post(`/integrations/github/tickets/${ticketId}/export`, {
      personalAccessToken: token,
      customComment,
    });
    return response.data;
  },
};