import { useState, useEffect } from "react";
import { toast } from "sonner";
import { githubService, GitHubIssue } from "@/api/services/githubService";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";

interface ImportGitHubModalProps {
  isOpen: boolean;
  onClose: () => void;
  sessionId: string;
  onImportSuccess?: () => void;
}

export default function ImportGitHubModal({
  isOpen,
  onClose,
  sessionId,
  onImportSuccess,
}: ImportGitHubModalProps) {
  const [repo, setRepo] = useState("");
  const [token, setToken] = useState("");
  const [issues, setIssues] = useState<GitHubIssue[]>([]);
  const [selectedIssues, setSelectedIssues] = useState<Record<number, boolean>>({});
  const [loading, setLoading] = useState(false);
  const [importing, setImporting] = useState(false);

  // Limpiar credenciales y estado cada vez que el modal se cierra o se abre de nuevo
  const resetForm = () => {
    setRepo("");
    setToken("");
    setIssues([]);
    setSelectedIssues({});
    setLoading(false);
    setImporting(false);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  // Limpiar estado sensible si isOpen cambia a false
  useEffect(() => {
    if (!isOpen) {
      resetForm();
    }
  }, [isOpen]);

  const handleRepoChange = (newRepo: string) => {
    setRepo(newRepo);
    // Si cambia el repo, limpiar la lista previa para evitar importar issues cruzados
    if (issues.length > 0) {
      setIssues([]);
      setSelectedIssues({});
    }
  };

  const handleFetchIssues = async () => {
    if (!repo.trim() || !repo.includes("/")) {
      toast.error("Formato inválido", {
        description: "El formato debe ser owner/repo (ej. facebook/react)",
      });
      return;
    }

    setLoading(true);
    try {
      const data = await githubService.fetchIssues(repo.trim(), token.trim() || undefined);
      setIssues(data);
      setSelectedIssues({});
      if (data.length === 0) {
        toast.info("No se encontraron issues abiertos");
      }
    } catch {
      // Error manejado en interceptor
    } finally {
      setLoading(false);
    }
  };

  const toggleSelectIssue = (issueNumber: number) => {
    setSelectedIssues((prev) => ({
      ...prev,
      [issueNumber]: !prev[issueNumber],
    }));
  };

  const handleToggleAll = () => {
    const allSelected = issues.every((i) => selectedIssues[i.number]);
    const nextState: Record<number, boolean> = {};
    if (!allSelected) {
      issues.forEach((i) => {
        nextState[i.number] = true;
      });
    }
    setSelectedIssues(nextState);
  };

  const handleImport = async () => {
    const chosen = issues.filter((i) => selectedIssues[i.number]);
    if (chosen.length === 0) {
      toast.warning("Selecciona al menos un issue para importar");
      return;
    }

    setImporting(true);
    try {
      // Ya NO se envía personalAccessToken porque el backend de importación no lo usa
      await githubService.importIssues({
        sessionId,
        repo: repo.trim(),
        issues: chosen,
      });

      toast.success(`${chosen.length} tickets importados correctamente`);
      if (onImportSuccess) onImportSuccess();
      handleClose();
    } catch {
      // Manejado por interceptor
    } finally {
      setImporting(false);
    }
  };

  const selectedCount = Object.values(selectedIssues).filter(Boolean).length;

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose()}>
      <DialogContent className="max-w-2xl max-h-[85vh] flex flex-col p-0 overflow-hidden">
        {/* Cabecera con DialogHeader */}
        <DialogHeader className="p-4 border-b border-border">
          <DialogTitle className="text-lg font-bold text-foreground">
            Importar Issues de GitHub
          </DialogTitle>
          <DialogDescription className="text-xs text-muted-foreground">
            Trae issues de un repositorio para estimarlos como tickets
          </DialogDescription>
        </DialogHeader>

        {/* Inputs de configuración */}
        <div className="p-4 border-b border-border bg-muted/20 space-y-3">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label 
                htmlFor="github-repo-input" 
                className="text-xs font-semibold text-muted-foreground block mb-1"
              >
                Repositorio (owner/repo) *
              </label>
              <input
                id="github-repo-input"
                type="text"
                placeholder="ej: facebook/react"
                value={repo}
                onChange={(e) => handleRepoChange(e.target.value)}
                className="w-full px-3 py-1.5 text-sm bg-background border border-border rounded-md text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>
            <div>
              <label 
                htmlFor="github-token-input" 
                className="text-xs font-semibold text-muted-foreground block mb-1"
              >
                Personal Access Token (PAT)
              </label>
              <input
                id="github-token-input"
                type="password"
                placeholder="Opcional para repos públicos"
                value={token}
                onChange={(e) => setToken(e.target.value)}
                className="w-full px-3 py-1.5 text-sm bg-background border border-border rounded-md text-foreground focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>
          </div>

          <Button
            size="sm"
            onClick={handleFetchIssues}
            disabled={loading}
            className="w-full md:w-auto"
          >
            {loading ? "Buscando..." : "Buscar Issues"}
          </Button>
        </div>

        {/* Lista de Issues */}
        <div className="flex-1 overflow-y-auto p-4 space-y-2">
          {issues.length > 0 && (
            <div className="flex items-center justify-between pb-2 border-b border-border">
              <button
                type="button"
                onClick={handleToggleAll}
                className="text-xs text-primary font-semibold hover:underline"
              >
                {issues.every((i) => selectedIssues[i.number])
                  ? "Deseleccionar todos"
                  : "Seleccionar todos"}
              </button>
              <span className="text-xs text-muted-foreground">
                {selectedCount} seleccionados
              </span>
            </div>
          )}

          {issues.length === 0 && !loading ? (
            <p className="text-center py-8 text-sm text-muted-foreground">
              Ingresa el repositorio y presiona "Buscar Issues" para listarlos.
            </p>
          ) : (
            issues.map((issue) => (
              <label
                key={issue.id}
                htmlFor={`issue-checkbox-${issue.number}`}
                className="flex items-start gap-3 p-3 bg-muted/30 hover:bg-muted/60 transition-colors border border-border rounded-lg cursor-pointer"
              >
                <input
                  id={`issue-checkbox-${issue.number}`}
                  type="checkbox"
                  checked={!!selectedIssues[issue.number]}
                  onChange={() => toggleSelectIssue(issue.number)}
                  className="mt-1 h-4 w-4 rounded border-border text-primary focus:ring-primary"
                />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">
                    #{issue.number} {issue.title}
                  </p>
                  {issue.body && (
                    <p className="text-xs text-muted-foreground line-clamp-2 mt-0.5">
                      {issue.body}
                    </p>
                  )}
                </div>
              </label>
            ))
          )}
        </div>

        {/* Footer */}
        <div className="p-3 border-t border-border flex items-center justify-between bg-muted/10">
          <Button variant="outline" size="sm" onClick={handleClose}>
            Cancelar
          </Button>
          <Button
            size="sm"
            onClick={handleImport}
            disabled={importing || selectedCount === 0}
          >
            {importing ? "Importando..." : `Importar (${selectedCount})`}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}