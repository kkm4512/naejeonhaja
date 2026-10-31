# ---------- 빌드 ----------
FROM node:22-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts

COPY . .
RUN npm run build

# ---------- 실행 ----------
FROM node:22-alpine
WORKDIR /app

ENV NODE_ENV=production \
    HOST=0.0.0.0 \
    PORT=3000 \
    NUXT_API_BASE=http://backend:8080

# Nuxt 빌드 결과(.output)는 node_modules 없이 단독 실행된다
COPY --from=build --chown=node:node /app/.output ./.output
USER node

EXPOSE 3000
HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
  CMD wget -qO- http://127.0.0.1:3000/ > /dev/null || exit 1

CMD ["node", ".output/server/index.mjs"]
