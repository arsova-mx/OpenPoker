package com.openpoker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubIssueDto {
    private Long id;
    private Integer number;
    private String title;
    private String body;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    private String state;
}