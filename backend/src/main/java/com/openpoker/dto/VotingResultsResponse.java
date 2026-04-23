package com.openpoker.dto;

import java.util.List;

public record VotingResultsResponse(String sessionCode, List<VoteResponse> votes, boolean revealed) {
}
