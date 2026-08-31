#!/usr/bin/env sh
set -eu

COMPOSE_FILE=${COMPOSE_FILE:-infra/compose.yaml}
ROLLBACK_API_IMAGE=${CREATORHUB_ROLLBACK_API_IMAGE:?CREATORHUB_ROLLBACK_API_IMAGE is required}
ROLLBACK_WEB_IMAGE=${CREATORHUB_ROLLBACK_WEB_IMAGE:?CREATORHUB_ROLLBACK_WEB_IMAGE is required}

if [ ! -f "$COMPOSE_FILE" ]; then
	echo "Compose file not found: $COMPOSE_FILE" >&2
	exit 1
fi

case "$ROLLBACK_API_IMAGE" in
	*:*) ;;
	*) echo "CREATORHUB_ROLLBACK_API_IMAGE must include an explicit tag" >&2; exit 1 ;;
esac
case "$ROLLBACK_WEB_IMAGE" in
	*:*) ;;
	*) echo "CREATORHUB_ROLLBACK_WEB_IMAGE must include an explicit tag" >&2; exit 1 ;;
esac

export CREATORHUB_API_IMAGE="$ROLLBACK_API_IMAGE"
export CREATORHUB_WEB_IMAGE="$ROLLBACK_WEB_IMAGE"

echo "Rolling back to ${ROLLBACK_API_IMAGE} and ${ROLLBACK_WEB_IMAGE}"
docker compose -f "$COMPOSE_FILE" up -d --no-build api web
docker compose -f "$COMPOSE_FILE" ps

echo "Waiting for API and Web health checks"
docker compose -f "$COMPOSE_FILE" up --wait api web