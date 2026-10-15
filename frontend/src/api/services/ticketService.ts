import { instance } from "@/api/clients/APIClient";

export type TicketStatus = "WAITING" | "VOTING" | "REVEALED" | "FINISHED";

export interface TicketResponse {
  id: string;
  title: string;
  description?: string;
  gameSessionId: string;
  status: TicketStatus;
  currentRound: number;
  externalSource?: string | null;
  externalId?: string | null;
  externalUrl?: string | null;
  externalRepo?: string | null;
  estimatedCardId?: string | null;
}

export interface CreateTicketRequest {
  title: string;
  description?: string;
  gameSessionId: string;
}

// Representa exactamente la carga útil que entrega el backend
interface RawTicketBackendResponse {
  id: string;
  title?: string;
  tittle?: string;
  description?: string;
  gameSessionId: string;
  status: TicketStatus;
  currentRound: number;
  externalSource?: string | null;
  externalId?: string | null;
  externalUrl?: string | null;
  externalRepo?: string | null;
  estimatedCardId?: string | null;
}

// Normaliza posibles inconsistencias de nombre de campo (title vs tittle)
// preservando externalSource y demás metadatos
const normalizeTicket = (raw: RawTicketBackendResponse): TicketResponse => ({
  id: raw.id,
  title: raw.title || raw.tittle || "Ticket sin título",
  description: raw.description,
  gameSessionId: raw.gameSessionId,
  status: raw.status,
  currentRound: raw.currentRound,
  externalSource: raw.externalSource,
  externalId: raw.externalId,
  externalUrl: raw.externalUrl,
  externalRepo: raw.externalRepo,
  estimatedCardId: raw.estimatedCardId,
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

  // PATCH /api/tickets/{ticketId}/status?newStatus=...&estimatedCardId=...
  updateStatus: async (
    ticketId: string, 
    newStatus: TicketStatus, 
    estimatedCardId?: string
  ): Promise<TicketResponse> => {
    const params: Record<string, string> = { newStatus };
    if (estimatedCardId) {
      params.estimatedCardId = estimatedCardId;
    }

    const response = await instance.patch<RawTicketBackendResponse>(
      `/tickets/${ticketId}/status`,
      null,
      { params }
    );
    return normalizeTicket(response.data);
  },
};