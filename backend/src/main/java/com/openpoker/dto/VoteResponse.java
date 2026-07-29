package com.openpoker.dto;

import java.sql.Timestamp;
import java.util.UUID;



public record VoteResponse(UUID voteId,String username, String cardValue, Timestamp votedAt) {
}
