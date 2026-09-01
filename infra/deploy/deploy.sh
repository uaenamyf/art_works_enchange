#!/usr/bin/env sh
set -eu

COMPOSE_FILE=${COMPOSE_FILE:-infra/compose.yaml}
COMPOSE_OVERRIDE_FILE=${COMPOSE_OVERRIDE_FILE:-infra/compose.prod.yaml}
COMPOSE_ARGS="-f $COMPOSE_FILE -f $COMPOSE_OVERRIDE_FILE"
API_IMAGE=${CREATORHUB_API_IMAGE:-creatorhub-api:local}
WEB_IMAGE=${CREATORHUB_WEB_IMAGE:-creatorhub-web:local}

# 部署前阻止明显的配置错误，避免应用启动后才暴露密钥或编排问题。
if [ ! -f "$COMPOSE_FILE" ]; then
	echo "Compose file not found: $COMPOSE_FILE" >&2
	exit 1
fi

if [ "${JWT_SECRET:-}" = "" ] || [ "${JWT_SECRET:-}" = "change-me-please-change-me-please-32bytes" ]; then
	echo "JWT_SECRET must be set to a non-placeholder value before deployment" >&2
	exit 1
fi

if [ "${#JWT_SECRET}" -lt 32 ]; then
	echo "JWT_SECRET must contain at least 32 characters" >&2
	exit 1
fi

case "$API_IMAGE" in
	*:*) ;;
	*) echo "CREATORHUB_API_IMAGE must include an explicit tag" >&2; exit 1 ;;
esac
case "$WEB_IMAGE" in
	*:*) ;;
	*) echo "CREATORHUB_WEB_IMAGE must include an explicit tag" >&2; exit 1 ;;
esac

export CREATORHUB_API_IMAGE="$API_IMAGE"
export CREATORHUB_WEB_IMAGE="$WEB_IMAGE"

echo "Building ${API_IMAGE} and ${WEB_IMAGE}"
docker compose $COMPOSE_ARGS build api web
docker compose $COMPOSE_ARGS up -d postgres redis minio api web
docker compose $COMPOSE_ARGS ps

echo "Waiting for API and Web health checks"
docker compose $COMPOSE_ARGS up --wait api web