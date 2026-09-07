// frontend/src/api/services/ticketService.ts
import { instance } from "@/api/clients/APIClient";

export interface TicketResponse {
  id: string;
  title: string;
  description?: string;
  gameSessionId: string;
  status?: "WAITING" | "VOTING" | "REVEALED" | "FINISHED";
}

export interface CreateTicketRequest {
  title: string;
  description?: string;
  gameSessionId: string;
}

export const ticketService = {
  // GET /api/tickets/session/{sessionId}
  getBySession: async (sessionId: string): Promise<TicketResponse[]> => {
    const response = await instance.get<TicketResponse[]>(`/tickets/session/${sessionId}`);
    return response.data;
  },

  // POST /api/tickets
  createTicket: async (data: CreateTicketRequest): Promise<TicketResponse> => {
    const response = await instance.post<TicketResponse>("/tickets", data);
    return response.data;
  },

  // PATCH /api/tickets/{ticketId}/status?newStatus=VOTING
  updateStatus: async (ticketId: string, newStatus: string): Promise<TicketResponse> => {
    const response = await instance.patch<TicketResponse>(
      `/tickets/${ticketId}/status?newStatus=${newStatus}`
    );
    return response.data;
  }
};