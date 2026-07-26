#!/usr/bin/env bash
set -e

CONTAINER_NAME="gateway-edge"
IMAGE_NAME="gateway-edge:latest"
COMPOSE_FILE="docker-compose.yml"

echo "==> Stopping compose..."
docker compose down --remove-orphans || true

echo "==> Removing old container..."
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

echo "==> Removing old image..."
docker rmi "$IMAGE_NAME" 2>/dev/null || true

echo "==> Removing dangling images..."
docker image prune -f >/dev/null

echo "==> Building and starting..."
docker compose up --build -d

echo
echo "==> Running containers"
docker ps

echo
echo "==> Logs"
docker logs -f "$CONTAINER_NAME"