-- Baseline del esquema de OpenPoker (PostgreSQL).
-- Generado a partir de las entidades JPA con el dialecto de PostgreSQL y revisado a mano:
-- las restricciones y llaves foráneas tienen nombres legibles para poder modificarlas en migraciones futuras.
-- Las bases de datos creadas antes con ddl-auto=update se registran en esta versión sin ejecutarla
-- (spring.flyway.baseline-on-migrate) y reciben las migraciones siguientes.

create table voting_deck (
    id          varchar(36)  not null,
    name        varchar(255) not null,
    series_type varchar(255) check (series_type in ('FIBONACCI', 'T_SHIRT', 'DOT_VOTING')),
    description varchar(500),
    constraint pk_voting_deck primary key (id)
);

create table card_value (
    id          varchar(36)  not null,
    deck_id     varchar(36),
    value       varchar(255) not null,
    order_index integer      not null,
    weight      integer,
    constraint pk_card_value primary key (id),
    constraint uk_card_value_deck_order unique (deck_id, order_index),
    constraint uk_card_value_deck_value unique (deck_id, value)
);

create table users (
    id            varchar(36)  not null,
    username      varchar(255) not null,
    email         varchar(255) not null,
    password_hash varchar(255) not null,
    role          varchar(255) check (role in ('HOST', 'VOTER')),
    company_name  varchar(255),
    phone_number  varchar(255),
    created_at    timestamp(6),
    updated_at    timestamp(6),
    constraint pk_users primary key (id),
    constraint uk_users_username unique (username),
    constraint uk_users_email unique (email)
);

create table game_sessions (
    id             varchar(36)  not null,
    session_code   varchar(255) not null,
    name           varchar(255) not null,
    host_id        varchar(36)  not null,
    deck_id        varchar(36),
    votes_revealed boolean      not null,
    created_at     timestamp(6),
    updated_at     timestamp(6),
    constraint pk_game_sessions primary key (id),
    constraint uk_game_sessions_session_code unique (session_code)
);

create table participants (
    id                 varchar(36) not null,
    game_session_id    varchar(36) not null,
    user_id            varchar(36),
    guest_display_name varchar(255),
    role               varchar(255) check (role in ('HOST', 'VOTER')),
    joined_at          timestamp(6),
    constraint pk_participants primary key (id)
);

create table ticket (
    id                varchar(36)  not null,
    id_session        varchar(36)  not null,
    title             varchar(255) not null,
    description       varchar(500),
    status            varchar(255) not null check (status in ('WAITING', 'VOTING', 'FINISHED', 'REVEALED')),
    estimated_card_id varchar(36),
    estimated_value   integer,
    duration_seconds  integer,
    timer_expires_at  timestamp(6) with time zone,
    constraint pk_ticket primary key (id)
);

create table votes (
    id             varchar(36) not null,
    ticket_id      varchar(36) not null,
    participant_id varchar(36) not null,
    card_value_id  varchar(36) not null,
    created_at     timestamp(6),
    updated_at     timestamp(6),
    constraint pk_votes primary key (id),
    constraint uk_votes_ticket_participant unique (ticket_id, participant_id)
);

create table comments (
    id         varchar(36)  not null,
    ticket_id  varchar(36)  not null,
    user_id    varchar(36)  not null,
    content    varchar(255) not null,
    created_at timestamp(6) not null,
    constraint pk_comments primary key (id)
);

alter table card_value
    add constraint fk_card_value_deck foreign key (deck_id) references voting_deck (id);

alter table game_sessions
    add constraint fk_game_sessions_deck foreign key (deck_id) references voting_deck (id);

alter table participants
    add constraint fk_participants_game_session foreign key (game_session_id) references game_sessions (id);
alter table participants
    add constraint fk_participants_user foreign key (user_id) references users (id);

alter table ticket
    add constraint fk_ticket_game_session foreign key (id_session) references game_sessions (id);
alter table ticket
    add constraint fk_ticket_estimated_card foreign key (estimated_card_id) references card_value (id);

alter table votes
    add constraint fk_votes_ticket foreign key (ticket_id) references ticket (id);
alter table votes
    add constraint fk_votes_participant foreign key (participant_id) references participants (id);
alter table votes
    add constraint fk_votes_card_value foreign key (card_value_id) references card_value (id);

alter table comments
    add constraint fk_comments_ticket foreign key (ticket_id) references ticket (id);
alter table comments
    add constraint fk_comments_user foreign key (user_id) references users (id);
