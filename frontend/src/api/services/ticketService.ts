import { instance } from "@/api/clients/APIClient";

export type TicketStatus = "WAITING" | "VOTING" | "REVEALED" | "FINISHED";

export interface TicketResponse {
  id: string;
  title: string;
  description?: string;
  gameSessionId: string;
  status: TicketStatus;
}

export interface CreateTicketRequest {
  title: string;
  description?: string;
  gameSessionId: string;
}

// Representa exactamente la carga útil que entrega el backend actualizado
interface RawTicketBackendResponse {
  id: string;
  title?: string;
  tittle?: string;
  description?: string;
  gameSessionId: string;
  status: TicketStatus;
}

// Normaliza posibles inconsistencias de nombre de campo (title vs tittle)
// preservando el estado real devuelto por el servidor
const normalizeTicket = (raw: RawTicketBackendResponse): TicketResponse => ({
  id: raw.id,
  title: raw.title || raw.tittle || "Ticket sin título",
  description: raw.description,
  gameSessionId: raw.gameSessionId,
  status: raw.status,
});

export const ticketService = {
  // GET /api/tickets/session/{sessionId}
  getBySession: async (sessionId: string): Promise<TicketResponse[]> => {
    const response = await instance.get<RawTicketBackendResponse[]>(`/tickets/session/${sessionId}`);
    return response.data.map(normalizeTicket);
  },

  // POST /api/tickets
  createTicket: async (data: CreateTicketRequest): Promise<TicketResponse> => {
    const response = await instance.post<RawTicketBackendResponse>("/tickets", data);
    return normalizeTicket(response.data);
  },

  // PATCH /api/tickets/{ticketId}/status?newStatus=...
  updateStatus: async (ticketId: string, newStatus: TicketStatus): Promise<TicketResponse> => {
    const response = await instance.patch<RawTicketBackendResponse>(
      `/tickets/${ticketId}/status`,
      null,
      { params: { newStatus } }
    );
    return normalizeTicket(response.data);
  },
};