#!/usr/bin/env sh
set -eu

COMPOSE_FILE=${COMPOSE_FILE:-infra/compose.yaml}
API_IMAGE=${CREATORHUB_API_IMAGE:-creatorhub-api:local}
WEB_IMAGE=${CREATORHUB_WEB_IMAGE:-creatorhub-web:local}

export CREATORHUB_API_IMAGE="$API_IMAGE"
export CREATORHUB_WEB_IMAGE="$WEB_IMAGE"

echo "Building ${API_IMAGE} and ${WEB_IMAGE}"
docker compose -f "$COMPOSE_FILE" build api web
docker compose -f "$COMPOSE_FILE" up -d postgres redis minio api web
docker compose -f "$COMPOSE_FILE" ps

echo "Waiting for API and Web health checks"
docker compose -f "$COMPOSE_FILE" up --wait api web