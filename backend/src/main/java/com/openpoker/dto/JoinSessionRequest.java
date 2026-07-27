package com.openpoker.dto;

public record JoinSessionRequest(
    String code,
    String username,
    String guestName
) {

}
