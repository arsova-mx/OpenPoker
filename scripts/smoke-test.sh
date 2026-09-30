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

# request METHOD URL [BODY] [TOKEN] -> escribe el body en $TMP/body e imprime el status HTTP
request() {
  local method="$1" url="$2" body="${3:-}" token="${4:-}"
  local args=(-sS -o "$TMP/body" -w '%{http_code}' -X "$method" -H 'Content-Type: application/json')
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
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
[[ "$(request GET "$API/sessions/$code" "" "$token")" == "200" ]] || fail "No se pudo consultar la sala $code"
pass "Sala $code creada y consultada con el token del registro"

# 6. Login con el usuario recién creado
status="$(request POST "$API/auth/login" "{\"username\":\"$user\",\"password\":\"smoke-pass-123\"}")"
[[ "$status" == "200" ]] || fail "El login respondió $status"
pass "Login"

echo "Smoke test OK"
