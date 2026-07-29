package com.openpoker.dto;

import java.sql.Timestamp;
import java.util.UUID;



public record VoteResponse(UUID id,String username, String cardValue, Timestamp votedAt) {
}
