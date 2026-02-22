# syntax=docker/dockerfile:1.7

# Build context is expected to be the repo root:
#   docker build -f apps/web/Dockerfile .

FROM node:20-bookworm-slim AS deps
WORKDIR /repo

COPY package.json package-lock.json ./
COPY apps/web/package.json ./apps/web/package.json

RUN npm ci

FROM deps AS builder
WORKDIR /repo

COPY apps/web ./apps/web

RUN npm -w apps/web run build

FROM node:20-bookworm-slim AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000

RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Next.js standalone output.
COPY --from=builder --chown=nextjs:nodejs /repo/apps/web/.next/standalone /app
COPY --from=builder --chown=nextjs:nodejs /repo/apps/web/.next/static /app/apps/web/.next/static
COPY --from=builder --chown=nextjs:nodejs /repo/apps/web/public /app/apps/web/public

# Defensive copy: some setups load message files at runtime (next-intl).
COPY --from=builder --chown=nextjs:nodejs /repo/apps/web/messages /app/apps/web/messages

USER nextjs

EXPOSE 3000

WORKDIR /app/apps/web

CMD ["node", "server.js"]
