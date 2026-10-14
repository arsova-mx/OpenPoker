import { instance } from "@/api/clients/APIClient";

export type SessionStatus = "ACTIVE" | "FINISHED" | "CANCELLED";
export type HistoryFilter = "ALL" | "HOST" | "PARTICIPANT";

export interface SessionHistorySummary {
  sessionId: string;
  sessionCode: string;
  name: string;
  status: SessionStatus;
  createdAt: string;
  closedAt?: string | null;
  isHost: boolean;
  totalTickets: number;
  estimatedTickets: number;
}

export interface TicketHistoryItem {
  id: string;
  title: string;
  description?: string | null;
  status: string;
  currentRound: number;
  finishedAt?: string | null;
  estimatedValue?: string | null;
}

// 👈 Exportada para poder tipar estados o props de paginación en tus componentes
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const historyService = {
  // GET /api/history/sessions?filter=...&page=...&size=...
  getMySessions: async (
    filter: HistoryFilter = "ALL", 
    page: number = 0, 
    size: number = 10
  ): Promise<PageResponse<SessionHistorySummary>> => {
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