# syntax=docker/dockerfile:1.7
# ---- deps stage ----
FROM node:22-alpine AS deps
WORKDIR /workspace
COPY package.json pnpm-lock.yaml* pnpm-workspace.yaml* ./
COPY apps/web/package.json apps/web/package.json
RUN corepack enable && pnpm install --frozen-lockfile || pnpm install

# ---- build stage ----
FROM node:22-alpine AS build
WORKDIR /workspace
COPY --from=deps /workspace/node_modules ./node_modules
COPY --from=deps /workspace/apps/web/node_modules ./apps/web/node_modules
COPY package.json pnpm-lock.yaml* pnpm-workspace.yaml* ./
COPY apps/web apps/web
ENV NEXT_TELEMETRY_DISABLED=1
RUN corepack enable && pnpm --filter @creatorhub/web build

# ---- runtime stage ----
FROM node:22-alpine
RUN addgroup -S creatorhub && adduser -S -G creatorhub -u 1001 creatorhub
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=build /workspace/apps/web/.next ./apps/web/.next
COPY --from=build /workspace/apps/web/public ./apps/web/public
COPY --from=build /workspace/apps/web/package.json ./apps/web/package.json
COPY --from=build /workspace/node_modules ./node_modules
USER creatorhub
EXPOSE 3000
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=10 \
  CMD wget -qO- http://localhost:3000/ >/dev/null 2>&1 || exit 1
WORKDIR /app/apps/web
CMD ["node", "node_modules/next/dist/bin/next", "start", "-p", "3000"]
