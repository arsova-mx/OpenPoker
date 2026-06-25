package com.openpoker.dto;

import java.sql.Timestamp;

import com.openpoker.entity.CardValue;

public record VoteResponse(String username, String cardValue, Timestamp votedAt) {
}
