package com.openpoker.dto;

import java.sql.Timestamp;



public record VoteResponse(String username, String cardValue, Timestamp votedAt) {
}
