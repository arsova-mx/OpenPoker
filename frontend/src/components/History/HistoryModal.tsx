import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { 
  historyService, 
  SessionHistorySummary, 
  TicketHistoryItem, 
  HistoryFilter 
} from "@/api/services/historyService";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";

interface HistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function HistoryModal({ isOpen, onClose }: HistoryModalProps) {
  const navigate = useNavigate();
  const [filter, setFilter] = useState<HistoryFilter>("ALL");
  const [sessions, setSessions] = useState<SessionHistorySummary[]>([]);
  const [loading, setLoading] = useState(false);

  // Paginación
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Sesión seleccionada para ver su desglose de tickets
  const [selectedSession, setSelectedSession] = useState<SessionHistorySummary | null>(null);
  const [tickets, setTickets] = useState<TicketHistoryItem[]>([]);
  const [loadingTickets, setLoadingTickets] = useState(false);

  // Reiniciar a la primera página cuando cambie el filtro
  const handleFilterChange = (newFilter: HistoryFilter) => {
    setFilter(newFilter);
    setPage(0);
  };

  useEffect(() => {
    if (isOpen) {
      loadSessions(page, filter);
    }
  }, [isOpen, page, filter]);

  const loadSessions = async (pageNumber: number, activeFilter: HistoryFilter) => {
    setLoading(true);
    try {
      const data = await historyService.getMySessions(activeFilter, pageNumber);
      setSessions(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch {
      // Manejado vía toast en interceptor
    } finally {
      setLoading(false);
    }
  };

  const handleSelectSession = async (session: SessionHistorySummary) => {
    setSelectedSession(session);
    setLoadingTickets(true);
    try {
      const ticketData = await historyService.getSessionTickets(session.sessionId);
      setTickets(ticketData);
    } catch {
      setTickets([]);
    } finally {
      setLoadingTickets(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl max-h-[85vh] flex flex-col p-0 overflow-hidden">
        {/* Cabecera accesible con DialogHeader */}
        <DialogHeader className="p-4 border-b border-border">
          <DialogTitle className="text-lg font-bold text-foreground">
            {selectedSession ? `Detalles: ${selectedSession.name}` : "Historial de Sesiones"}
          </DialogTitle>
          <DialogDescription className="text-xs text-muted-foreground">
            {selectedSession
              ? `Código: ${selectedSession.sessionCode} • ${selectedSession.isHost ? "Fuiste Host" : "Participaste"}`
              : "Revisa las sesiones que creaste o en las que votaste"}
          </DialogDescription>
        </DialogHeader>

        {/* Vista 1: Detalle de Tickets de una sesión seleccionada */}
        {selectedSession ? (
          <div className="p-4 flex-1 overflow-y-auto space-y-4">
            <div className="flex items-center justify-between">
              <Button variant="outline" size="sm" onClick={() => setSelectedSession(null)}>
                ← Volver a sesiones
              </Button>

              {/* Solo mostrar el botón si la sala está ACTIVA */}
              {selectedSession.status === "ACTIVE" && (
                <Button 
                  size="sm" 
                  onClick={() => {
                    onClose();
                    navigate(`/session/${selectedSession.sessionCode}`);
                  }}
                >
                  Entrar a la sala
                </Button>
              )}
            </div>

            {loadingTickets ? (
              <p className="text-center py-8 text-sm text-muted-foreground">Cargando tickets...</p>
            ) : tickets.length === 0 ? (
              <p className="text-center py-8 text-sm text-muted-foreground">
                No se registraron tickets en esta sesión.
              </p>
            ) : (
              <div className="space-y-2">
                {tickets.map((t) => (
                  <div
                    key={t.id}
                    className="p-3 bg-muted/40 rounded-lg border border-border flex items-center justify-between gap-4"
                  >
                    <div className="min-w-0">
                      <p className="font-semibold text-sm text-foreground truncate">{t.title}</p>
                      {t.description && (
                        <p className="text-xs text-muted-foreground line-clamp-1">{t.description}</p>
                      )}
                      <span className="text-[10px] uppercase font-bold text-muted-foreground">
                        Ronda {t.currentRound} • Estado: {t.status}
                      </span>
                    </div>
                    <div className="shrink-0 text-right">
                      <span className="text-xs text-muted-foreground block">Estimado</span>
                      <span className="inline-block px-2 py-1 text-sm font-bold bg-primary text-primary-foreground rounded">
                        {t.estimatedValue || "—"}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ) : (
          /* Vista 2: Lista general de sesiones con filtros y paginación */
          <div className="flex flex-col flex-1 overflow-hidden">
            {/* Filtros */}
            <div className="flex gap-2 p-3 border-b border-border bg-muted/20">
              <Button
                variant={filter === "ALL" ? "default" : "outline"}
                size="sm"
                onClick={() => handleFilterChange("ALL")}
              >
                Todas
              </Button>
              <Button
                variant={filter === "HOST" ? "default" : "outline"}
                size="sm"
                onClick={() => handleFilterChange("HOST")}
              >
                Mis Salas (Host)
              </Button>
              <Button
                variant={filter === "PARTICIPANT" ? "default" : "outline"}
                size="sm"
                onClick={() => handleFilterChange("PARTICIPANT")}
              >
                Participadas
              </Button>
            </div>

            {/* Listado de sesiones */}
            <div className="flex-1 overflow-y-auto p-4 space-y-2">
              {loading ? (
                <p className="text-center py-8 text-sm text-muted-foreground">Cargando historial...</p>
              ) : sessions.length === 0 ? (
                <div className="text-center py-10 space-y-1">
                  <p className="text-sm font-medium text-foreground">No tienes sesiones registradas</p>
                  <p className="text-xs text-muted-foreground">Las salas donde seas host o votes aparecerán aquí.</p>
                </div>
              ) : (
                sessions.map((session) => (
                  <button
                    key={session.sessionId}
                    type="button"
                    onClick={() => handleSelectSession(session)}
                    className="w-full text-left p-3.5 bg-card hover:bg-muted/50 focus:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-primary transition-colors border border-border rounded-lg flex items-center justify-between gap-4"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-semibold text-sm text-foreground">{session.name}</span>
                        <span
                          className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                            session.isHost
                              ? "bg-amber-500/20 text-amber-600 dark:text-amber-400"
                              : "bg-blue-500/20 text-blue-600 dark:text-blue-400"
                          }`}
                        >
                          {session.isHost ? "Host" : "Participante"}
                        </span>
                        <span className="text-[10px] px-1.5 py-0.5 rounded bg-muted text-muted-foreground font-mono uppercase">
                          {session.status}
                        </span>
                      </div>
                      <p className="text-xs text-muted-foreground mt-1">
                        Código: <strong className="text-foreground tracking-wider">{session.sessionCode}</strong> •{" "}
                        {new Date(session.createdAt).toLocaleDateString()}
                      </p>
                    </div>

                    <div className="text-right shrink-0">
                      <span className="text-xs font-medium text-muted-foreground block">
                        {session.estimatedTickets} / {session.totalTickets} tickets
                      </span>
                      <span className="text-xs text-primary font-semibold">
                        Ver tickets →
                      </span>
                    </div>
                  </button>
                ))
              )}
            </div>

            {/* Controles de Paginación */}
            {totalPages > 1 && (
              <div className="p-3 border-t border-border flex items-center justify-between bg-muted/10">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                  disabled={page === 0 || loading}
                >
                  ← Anterior
                </Button>
                <span className="text-xs text-muted-foreground">
                  Página {page + 1} de {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((prev) => Math.min(prev + 1, totalPages - 1))}
                  disabled={page >= totalPages - 1 || loading}
                >
                  Siguiente →
                </Button>
              </div>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}