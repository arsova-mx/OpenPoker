import { useState } from "react";
import { toast } from "sonner";
import { githubService } from "@/api/services/githubService";
import { Button } from "@/components/ui/button";

interface ExportGitHubModalProps {
  isOpen: boolean;
  onClose: () => void;
  ticketId: string;
  ticketTitle: string;
}

export default function ExportGitHubModal({
  isOpen,
  onClose,
  ticketId,
  ticketTitle,
}: ExportGitHubModalProps) {
  const [token, setToken] = useState("");
  const [customComment, setCustomComment] = useState("");
  const [loading, setLoading] = useState(false);

  if (!isOpen) return null;

  const handleExport = async () => {
    if (!token.trim()) {
      toast.error("Token requerido", {
        description: "Se necesita un Personal Access Token para comentar en GitHub",
      });
      return;
    }

    setLoading(true);
    try {
      await githubService.exportEstimation(ticketId, token.trim(), customComment.trim() || undefined);
      toast.success("Comentario publicado exitosamente en GitHub");
      onClose();
    } catch {
      // Manejado por interceptor
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div className="w-full max-w-md bg-card border border-border rounded-xl shadow-2xl p-4 space-y-4">
        <div>
          <h2 className="text-lg font-bold text-foreground">Exportar a GitHub</h2>
          <p className="text-xs text-muted-foreground truncate">
            Ticket: {ticketTitle}
          </p>
        </div>

        <div className="space-y-3">
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1">
              Personal Access Token (con permisos de issues) *
            </label>
            <input
              type="password"
              placeholder="ghp_..."
              value={token}
              onChange={(e) => setToken(e.target.value)}
              className="w-full px-3 py-1.5 text-sm bg-background border border-border rounded-md text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1">
              Comentario personalizado (opcional)
            </label>
            <textarea
              rows={3}
              placeholder="Deja vacío para usar el formato por defecto de OpenPoker"
              value={customComment}
              onChange={(e) => setCustomComment(e.target.value)}
              className="w-full px-3 py-1.5 text-sm bg-background border border-border rounded-md text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>
        </div>

        <div className="flex items-center justify-end gap-2 pt-2 border-t border-border">
          <Button variant="outline" size="sm" onClick={onClose}>
            Cancelar
          </Button>
          <Button size="sm" onClick={handleExport} disabled={loading}>
            {loading ? "Publicando..." : "Publicar Comentario"}
          </Button>
        </div>
      </div>
    </div>
  );
}