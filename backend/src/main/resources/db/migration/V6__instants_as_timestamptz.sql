-- Los campos java.time.Instant agregados en V3 se crearon como timestamp sin zona horaria. Se guardan como
-- timestamp with time zone, igual que ticket.timer_expires_at (V1), para que el instante guardado no dependa
-- de la zona horaria del servidor. PostgreSQL convierte los valores existentes con la zona horaria de la
-- sesión, que es la misma con la que la aplicación los escribió.
alter table game_sessions alter column closed_at set data type timestamp(6) with time zone;
alter table participants alter column left_at set data type timestamp(6) with time zone;
alter table ticket alter column created_at set data type timestamp(6) with time zone;
alter table ticket alter column finished_at set data type timestamp(6) with time zone;
