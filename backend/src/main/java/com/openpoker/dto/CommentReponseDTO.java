package com.openpoker.dto;

import java.util.UUID;

public record CommentReponseDTO(
    UUID id,
    String username,
    String content,
    java.sql.Timestamp createdAt
) {

}
