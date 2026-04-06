package com.openpoker.entity;

/**
 * Placeholder package for JPA entities.
 *
 * <p>Entities to be created during development:
 * <ul>
 *   <li>{@code Session}     – a planning poker session (room)</li>
 *   <li>{@code Story}       – a user story within a session</li>
 *   <li>{@code Participant} – a user who has joined a session</li>
 *   <li>{@code Vote}        – a single card vote cast by a participant</li>
 * </ul>
 *
 * <p>All entities will use UUID primary keys and include audit fields
 * ({@code createdAt}, {@code updatedAt}).
 */
public final class EntityPlaceholder {
    private EntityPlaceholder() {}
}
