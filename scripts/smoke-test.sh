#!/usr/bin/env bash
# Smoke test de extremo a extremo contra un stack levantado con `docker compose up`.
# Pasa por el origen del frontend (nginx) para validar también el proxy de /api.
#
# Uso: scripts/smoke-test.sh [BASE_URL]   (por defecto http://localhost:3000)
# Requiere: curl y jq.
set -euo pipefail

BASE_URL="${1:-http://localhost:3000}"
API="$BASE_URL/api"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

pass() { printf '  \033[32m✔\033[0m %s\n' "$1"; }
fail() { printf '  \033[31m✘\033[0m %s\n' "$1" >&2; exit 1; }

# request METHOD URL [BODY] [TOKEN] [ORIGIN] -> escribe el body en $TMP/body e imprime el status HTTP
request() {
  local method="$1" url="$2" body="${3:-}" token="${4:-}" origin="${5:-}"
  local args=(-sS -o "$TMP/body" -w '%{http_code}' -X "$method" -H 'Content-Type: application/json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$origin" ]] && args+=(-H "Origin: $origin")
  [[ -n "$body" ]] && args+=(--data "$body")
  curl "${args[@]}" "$url"
}

echo "Smoke test contra $BASE_URL"

# 1. El frontend responde y el fallback de SPA funciona
[[ "$(request GET "$BASE_URL/")" == "200" ]] || fail "GET / no respondió 200"
[[ "$(request GET "$BASE_URL/ruta/que/no/existe")" == "200" ]] || fail "El fallback de SPA no respondió 200"
pass "Frontend y fallback de SPA"

# 2. El bundle no apunta a localhost (#43)
bundle_path="$(grep -oE '/assets/index-[A-Za-z0-9_-]+\.js' "$TMP/body" | head -1 || true)"
[[ -n "$bundle_path" ]] || fail "No se encontró el bundle JS en index.html"
curl -sS "$BASE_URL$bundle_path" -o "$TMP/bundle.js"
if grep -q 'localhost:8080' "$TMP/bundle.js"; then fail "El bundle contiene localhost:8080"; fi
pass "El bundle no apunta a localhost"

# 3. Las barajas existen y tienen cartas (#41)
[[ "$(request GET "$API/card-decks")" == "200" ]] || fail "GET /api/card-decks no respondió 200"
deck_count="$(jq 'length' "$TMP/body")"
empty_decks="$(jq '[.[] | select((.cards | length) == 0)] | length' "$TMP/body")"
[[ "$deck_count" -ge 3 ]] || fail "Se esperaban al menos 3 barajas y hay $deck_count"
[[ "$empty_decks" -eq 0 ]] || fail "$empty_decks baraja(s) sin cartas"
pass "$deck_count barajas sembradas, todas con cartas"

# 3b. CORS a través de nginx (#57): el origen del propio frontend funciona y uno ajeno se rechaza
[[ "$(request GET "$API/card-decks" "" "" "$BASE_URL")" == "200" ]] || fail "Una petición desde el origen del frontend fue rechazada"
[[ "$(request GET "$API/card-decks" "" "" "https://evil.example.com")" == "403" ]] || fail "Una petición desde un origen ajeno no fue rechazada"
pass "CORS: origen del frontend permitido, origen ajeno rechazado"

# 3c. Detrás de un proxy que termina TLS (p. ej. Cloudflare): nginx conserva X-Forwarded-Proto y el
#     backend reconoce el mismo origen aunque no esté en ALLOWED_ORIGINS
behind_tls() {
  curl -sS -o /dev/null -w '%{http_code}' -H 'Host: openpoker.example.com' -H 'X-Forwarded-Proto: https' \
    -H "Origin: $1" "$API/card-decks"
}
[[ "$(behind_tls https://openpoker.example.com)" == "200" ]] || fail "El mismo origen detrás de un proxy TLS fue rechazado"
[[ "$(behind_tls https://evil.example.com)" == "403" ]] || fail "Un origen ajeno detrás de un proxy TLS no fue rechazado"
pass "CORS detrás de un proxy TLS: mismo origen reconocido, origen ajeno rechazado"

# 4. El registro devuelve un token utilizable (#45)
user="smoke$(date +%s)$RANDOM"
status="$(request POST "$API/auth/register" "{\"username\":\"$user\",\"email\":\"$user@example.com\",\"password\":\"smoke-pass-123\"}")"
[[ "$status" == "201" ]] || fail "El registro respondió $status: $(cat "$TMP/body")"
token="$(jq -r '.token // empty' "$TMP/body")"
[[ -n "$token" && "$token" != "null" ]] || fail "El registro no devolvió token"
pass "Registro devuelve token"

# 5. Con ese token se puede crear una sala
status="$(request POST "$API/sessions" '{"name":"Smoke test"}' "$token")"
[[ "$status" == "201" ]] || fail "Crear sala respondió $status: $(cat "$TMP/body")"
code="$(jq -r '.sessionCode' "$TMP/body")"
session_id="$(jq -r '.id' "$TMP/body")"
deck_id="$(jq -r '.deckId' "$TMP/body")"
[[ "$(request GET "$API/sessions/$code" "" "$token")" == "200" ]] || fail "No se pudo consultar la sala $code"
pass "Sala $code creada y consultada con el token del registro"

# 6. Votación completa: ticket → VOTING → voto "5" → reveal con estadísticas y carta sugerida (#41)
status="$(request POST "$API/tickets" "{\"title\":\"Smoke ticket\",\"gameSessionId\":\"$session_id\"}" "$token")"
[[ "$status" == "200" || "$status" == "201" ]] || fail "Crear ticket respondió $status: $(cat "$TMP/body")"
ticket_id="$(jq -r '.id' "$TMP/body")"
status="$(request PATCH "$API/tickets/$ticket_id/status?newStatus=VOTING" "" "$token")"
[[ "$status" == "200" ]] || fail "Pasar el ticket a VOTING respondió $status"
[[ "$(request GET "$API/card-decks")" == "200" ]] || fail "No se pudieron leer las barajas"
card_id="$(jq -r --arg d "$deck_id" '.[] | select(.id == $d) | .cards[] | select(.value == "5") | .id' "$TMP/body")"
[[ -n "$card_id" ]] || fail "La baraja de la sala no tiene la carta 5"
status="$(request POST "$API/sessions/$code/votes?ticketId=$ticket_id" "{\"cardValue\":\"$card_id\"}" "$token")"
[[ "$status" == "200" ]] || fail "Votar respondió $status: $(cat "$TMP/body")"
status="$(request POST "$API/sessions/$code/votes/reveal?ticketId=$ticket_id" "" "$token")"
[[ "$status" == "200" ]] || fail "Revelar respondió $status: $(cat "$TMP/body")"
average="$(jq -r '.statistics.average' "$TMP/body")"
suggested="$(jq -r '.suggestedCardValue' "$TMP/body")"
[[ "$average" == "5" || "$average" == "5.0" ]] || fail "El promedio debía ser 5 y fue $average"
[[ "$suggested" == "5" ]] || fail "La carta sugerida debía ser 5 y fue $suggested"
pass "Votación y reveal: promedio $average, carta sugerida $suggested"

# 7. Login con el usuario recién creado
status="$(request POST "$API/auth/login" "{\"username\":\"$user\",\"password\":\"smoke-pass-123\"}")"
[[ "$status" == "200" ]] || fail "El login respondió $status"
pass "Login"

echo "Smoke test OK"
