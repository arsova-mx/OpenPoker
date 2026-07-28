package com.openpoker.dto;

import java.util.List;

public record VotingRRAverage(
    String sessionCode,
    List<VoteResponse> votes,
    boolean revealed,
    double suggestedAverage, // Promedio calculado
    String suggestedCardValue, // Valor sugerido (ej. "M" o "5")) {
    VoteStatisticsDTO statistics)
{}