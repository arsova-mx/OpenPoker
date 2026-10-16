import { useState, useEffect } from "react";
import { toast } from "sonner";
import { githubService } from "@/api/services/githubService";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";

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

  const resetForm = () => {
    setToken("");
    setCustomComment("");
    setLoading(false);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  useEffect(() => {
    if (!isOpen) {
      resetForm();
    }
  }, [isOpen]);

  const handleExport = async () => {
    if (!token.trim()) {
      toast.error("Token requerido", {
        description: "Se necesita un Personal Access Token para comentar en GitHub",
      });
      return;
    }

    setLoading(true);
    try {
      await githubService.exportEstimation(
        ticketId, 
        token.trim(), 
        customComment.trim() || undefined
      );
      toast.success("Comentario publicado exitosamente en GitHub");
      handleClose();
    } catch {
      // Manejado por interceptor
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose()}>
      <DialogContent className="sm:max-w-[480px] p-6">
        <DialogHeader className="text-left space-y-1">
          <DialogTitle className="text-lg font-bold text-foreground">
            Exportar a GitHub
          </DialogTitle>
          <DialogDescription className="text-xs text-muted-foreground line-clamp-2">
            Ticket: {ticketTitle}
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4 py-2 w-full">
          <div className="flex flex-col gap-1.5 w-full">
            <label 
              htmlFor="github-export-token" 
              className="text-xs font-semibold text-muted-foreground"
            >
              Personal Access Token (con permisos de issues) *
            </label>
            <input
              id="github-export-token"
              type="password"
              placeholder="ghp_..."
              value={token}
              onChange={(e) => setToken(e.target.value)}
              className="w-full box-border px-3 py-2 text-sm bg-background border border-border rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all"
            />
          </div>

          <div className="flex flex-col gap-1.5 w-full">
            <label 
              htmlFor="github-export-comment" 
              className="text-xs font-semibold text-muted-foreground"
            >
              Comentario personalizado (opcional)
            </label>
            <textarea
              id="github-export-comment"
              rows={3}
              placeholder="Deja vacío para usar el formato por defecto de OpenPoker"
              value={customComment}
              onChange={(e) => setCustomComment(e.target.value)}
              className="w-full box-border px-3 py-2 text-sm bg-background border border-border rounded-md text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent resize-none transition-all"
            />
          </div>
        </div>

        <DialogFooter className="border-t border-border pt-4 flex flex-row items-center justify-end gap-2 w-full">
          <Button variant="outline" size="sm" onClick={handleClose} disabled={loading}>
            Cancelar
          </Button>
          <Button size="sm" onClick={handleExport} disabled={loading || !token.trim()}>
            {loading ? "Publicando..." : "Publicar Comentario"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}