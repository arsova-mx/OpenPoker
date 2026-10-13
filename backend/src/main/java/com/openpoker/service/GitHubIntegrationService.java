package com.openpoker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.openpoker.dto.GitHubIssueDto;

@Service
public class GitHubIntegrationService {

    private final RestClient restClient;
    private static final int MAX_PAGES = 5; // Límite defensivo: hasta 500 elementos para cuidar el rate limit

    public GitHubIntegrationService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    /**
     * Consulta los issues abiertos de un repositorio de GitHub (owner/repo).
     * Excluye Pull Requests y recorre la paginación de la API.
     */
    public List<GitHubIssueDto> fetchOpenIssues(String repo, String token) {
        String[] parts = repo.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("El formato del repositorio debe ser 'owner/repo'");
        }

        List<GitHubIssueDto> openIssues = new ArrayList<>();
        int page = 1;

        while (page <= MAX_PAGES) {
            final int currentPage = page;
            var spec = restClient.get()
                    .uri("/repos/{owner}/{repo}/issues?state=open&per_page=100&page={page}", 
                            parts[0], parts[1], currentPage);

            if (token != null && !token.isBlank()) {
                spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
            }

            GitHubIssueDto[] batch = spec.retrieve().body(GitHubIssueDto[].class);

            if (batch == null || batch.length == 0) {
                break;
            }

            // 1. Filtrar solo los que NO sean Pull Requests
            List<GitHubIssueDto> filtered = Arrays.stream(batch)
                    .filter(issue -> issue.getPullRequest() == null)
                    .toList();

            openIssues.addAll(filtered);

            // 2. Si devolvió menos de 100, significa que era la última página
            if (batch.length < 100) {
                break;
            }

            page++;
        }

        return openIssues;
    }

    /**
     * Publica un comentario en el issue con el resultado de la estimación.
     */
    public void postEstimationComment(String repo, String issueNumber, String commentText, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Se requiere un Personal Access Token para exportar a GitHub");
        }

        String[] parts = repo.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("El formato del repositorio debe ser 'owner/repo'");
        }

        restClient.post()
                .uri("/repos/{owner}/{repo}/issues/{issue_number}/comments", parts[0], parts[1], issueNumber)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("body", commentText))
                .retrieve()
                .toBodilessEntity();
    }
}