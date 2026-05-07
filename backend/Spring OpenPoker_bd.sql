create database openpoker_bd;
use openpoker_bd;
select * from game_sessions;
select * from votes;
select * from participants;
select * from users;
select * from game_sessions;
select * from voting_deck;
select * from deck_value;
drop database openpoker_bd;

-- 1) Deck por defecto
INSERT INTO voting_deck (id, name)
VALUES (UUID_TO_BIN(UUID()), 'Fibonacci');

SELECT BIN_TO_UUID(id), name FROM voting_deck;

-- 2) Valores del deck
INSERT INTO deck_value (id, value, deck_id)
SELECT 
    UUID_TO_BIN(UUID()),  -- nuevo id para cada deck_value
    fib.val,              -- valor Fibonacci
    vd.id                 -- FK al deck (ya en BINARY(16))
FROM voting_deck vd
JOIN (
		
    SELECT '0' AS val UNION ALL
    SELECT '1' UNION ALL
    SELECT '2' UNION ALL
    SELECT '3' UNION ALL
    SELECT '5' UNION ALL
    SELECT '8' UNION ALL
    SELECT '13' UNION ALL
    SELECT '21' UNION ALL
    SELECT '34' UNION ALL
    SELECT '55' UNION ALL
    SELECT '89' UNION ALL
    SELECT '?'
) AS fib
WHERE vd.name = 'Fibonacci';

SELECT 
    BIN_TO_UUID(id) AS id,
    value,
    BIN_TO_UUID(deck_id) AS deck_id
FROM deck_value;
