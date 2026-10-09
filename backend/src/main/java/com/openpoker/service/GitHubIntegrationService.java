package com.openpoker.service;

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

    public GitHubIntegrationService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    /**
     * Consulta los issues abiertos de un repositorio de GitHub (owner/repo).
     */
    public List<GitHubIssueDto> fetchOpenIssues(String repo, String token) {
        String[] parts = repo.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("El formato del repositorio debe ser 'owner/repo'");
        }

        var spec = restClient.get()
                .uri("/repos/{owner}/{repo}/issues?state=open&per_page=50", parts[0], parts[1]);

        if (token != null && !token.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
        }

        GitHubIssueDto[] result = spec.retrieve().body(GitHubIssueDto[].class);
        return result != null ? Arrays.asList(result) : List.of();
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