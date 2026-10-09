import { instance } from "@/api/clients/APIClient";

export type SessionStatus = "ACTIVE" | "FINISHED" | "CANCELLED";
export type HistoryFilter = "ALL" | "HOST" | "PARTICIPANT";

export interface SessionHistorySummary {
  sessionId: string;
  sessionCode: string;
  name: string;
  status: SessionStatus;
  createdAt: string;
  closedAt?: string;
  isHost: boolean;
  totalTickets: number;
  estimatedTickets: number;
}

export interface TicketHistoryItem {
  id: string;
  title: string;
  description?: string;
  status: string;
  currentRound: number;
  finishedAt?: string;
  estimatedValue?: string;
}

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const historyService = {
  // GET /api/history/sessions?filter=...&page=...
  getMySessions: async (filter: HistoryFilter = "ALL", page = 0, size = 10): Promise<PageResponse<SessionHistorySummary>> => {
    const response = await instance.get<PageResponse<SessionHistorySummary>>("/history/sessions", {
      params: { filter, page, size },
    });
    return response.data;
  },

  // GET /api/history/sessions/{sessionId}/tickets
  getSessionTickets: async (sessionId: string): Promise<TicketHistoryItem[]> => {
    const response = await instance.get<TicketHistoryItem[]>(`/history/sessions/${sessionId}/tickets`);
    return response.data;
  },
};