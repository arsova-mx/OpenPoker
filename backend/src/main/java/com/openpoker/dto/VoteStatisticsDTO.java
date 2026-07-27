package com.openpoker.dto;

import java.util.List;
import java.util.UUID;

public record VoteStatisticsDTO(
    double average,
    double consensusPercentage,
    boolean isFullConsensus,
    List<UUID> outlierVoteIds
) {}
