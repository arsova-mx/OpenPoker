package com.openpoker.dto;

import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class ImportGitHubIssuesRequest {
    private UUID sessionId;
    private String repo; // "owner/repo"
    private String personalAccessToken; // Opcional si el repo es público
    private List<GitHubIssueDto> issues; // Los issues seleccionados con checkbox
}