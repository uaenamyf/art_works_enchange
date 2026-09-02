# ---- build stage ----
FROM node:22-alpine AS build
WORKDIR /workspace
ENV PNPM_HOME=/pnpm
ENV PATH=$PNPM_HOME:$PATH
ARG NPM_REGISTRY=https://registry.npmmirror.com
ENV npm_config_registry=${NPM_REGISTRY}
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
COPY apps/web/package.json apps/web/package.json
RUN --mount=type=cache,id=creatorhub-pnpm-store,target=/pnpm/store \
  corepack enable \
  && pnpm install --frozen-lockfile \
    --registry=${NPM_REGISTRY} \
    --config.node-linker=hoisted

COPY apps/web apps/web
ENV NEXT_TELEMETRY_DISABLED=1
ENV NEXT_OUTPUT=standalone
# NEXT_PUBLIC_* env vars are inlined into the browser bundle at build time,
# so this must be a browser-reachable URL, not the Docker-only `api` hostname.
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV API_INTERNAL_BASE_URL=http://api:8080
RUN --mount=type=cache,id=creatorhub-pnpm-store,target=/pnpm/store \
  corepack enable \
  && pnpm --filter @creatorhub/web build

# ---- runtime stage ----
FROM node:22-alpine
RUN addgroup -S creatorhub && adduser -S -G creatorhub -u 1001 creatorhub
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV API_INTERNAL_BASE_URL=http://api:8080
COPY --from=build --chown=creatorhub:creatorhub /workspace/apps/web/.next/standalone ./
COPY --from=build --chown=creatorhub:creatorhub /workspace/apps/web/.next/static ./apps/web/.next/static
USER creatorhub
EXPOSE 3000
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
  CMD wget -qO- http://127.0.0.1:3000/ >/dev/null 2>&1 || exit 1
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
CMD ["node", "apps/web/server.js"]
