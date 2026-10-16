package com.openpoker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryGitHubIssuesRequest {
    private String repo;
    private String personalAccessToken;
}