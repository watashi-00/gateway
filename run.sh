#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="gateway-edge"
IMAGE_NAME="gateway-edge:latest"
COMPOSE_FILE="docker-compose.yml"
MAVEN_SRC="/home/watashi/.m2/repository/io/hexacloud/gatebridge-core/1.4.9-release"
MAVEN_DEST="local-maven-repo/io/hexacloud/gatebridge-core/1.4.9-release"

LOG_DIR="./.deploy-logs"
BUILD_LOG="$LOG_DIR/build.log"
mkdir -p "$LOG_DIR"
: > "$BUILD_LOG"   # reset the log on every run

C_RESET='\033[0m'
C_BLUE='\033[1;34m'
C_GREEN='\033[1;32m'
C_YELLOW='\033[1;33m'
C_RED='\033[1;31m'
C_DIM='\033[2m'

ts() { date '+%H:%M:%S'; }

section() { echo -e "\n${C_BLUE}==>${C_RESET} ${C_BLUE}$*${C_RESET}"; }
info()    { echo -e "${C_DIM}[$(ts)]${C_RESET} $*"; }
ok()      { echo -e "${C_DIM}[$(ts)]${C_RESET} ${C_GREEN}✔${C_RESET} $*"; }
warn()    { echo -e "${C_DIM}[$(ts)]${C_RESET} ${C_YELLOW}⚠${C_RESET} $*"; }
fail()    { echo -e "${C_DIM}[$(ts)]${C_RESET} ${C_RED}✘${C_RESET} $*"; }

run_quiet() {
    local desc="$1"; shift
    local start end
    start=$(date +%s)

    echo -ne "${C_DIM}[$(ts)]${C_RESET} ${desc}... "

    if "$@" >> "$BUILD_LOG" 2>&1; then
        end=$(date +%s)
        echo -e "${C_GREEN}done${C_RESET} ${C_DIM}(${desc}, $((end - start))s)${C_RESET}"
        return 0
    else
        local code=$?
        end=$(date +%s)
        echo -e "${C_RED}failed${C_RESET} ${C_DIM}($((end - start))s)${C_RESET}"
        fail "'${desc}' failed. Last lines of the log (${BUILD_LOG}):"
        echo -e "${C_DIM}--------------------------------------------${C_RESET}"
        tail -n 25 "$BUILD_LOG"
        echo -e "${C_DIM}--------------------------------------------${C_RESET}"
        return $code
    fi
}

on_error() {
    local line=$1
    fail "Error on line ${line}. Full log at: ${BUILD_LOG}"
}
trap 'on_error $LINENO' ERR

section "Cleaning up old environment"

run_quiet "Tearing down compose" docker compose -f "$COMPOSE_FILE" down --remove-orphans

if docker rm -f "$CONTAINER_NAME" >> "$BUILD_LOG" 2>&1; then
    ok "Removed old container ($CONTAINER_NAME)"
else
    info "No old container to remove ($CONTAINER_NAME)"
fi

if docker rmi "$IMAGE_NAME" >> "$BUILD_LOG" 2>&1; then
    ok "Removed old image ($IMAGE_NAME)"
else
    info "No old image to remove ($IMAGE_NAME)"
fi

run_quiet "Pruning dangling images" docker image prune -f

section "Syncing gatebridge-core (local)"

if [ ! -d "$MAVEN_SRC" ]; then
    fail "Source directory not found: $MAVEN_SRC"
    exit 1
fi

mkdir -p "$MAVEN_DEST"
cp "$MAVEN_SRC"/* "$MAVEN_DEST"/
n_files=$(find "$MAVEN_DEST" -maxdepth 1 -type f | wc -l | tr -d ' ')
ok "Synced ($n_files file(s)) -> $MAVEN_DEST"

section "Building and starting containers"
info "Detailed build output goes to ${BUILD_LOG} (won't spam the terminal)"

run_quiet "docker compose up --build -d" docker compose -f "$COMPOSE_FILE" up --build -d

section "Running containers"
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'

section "Logs for ${CONTAINER_NAME} (Ctrl+C to exit)"
docker logs -f --tail 50 "$CONTAINER_NAME"