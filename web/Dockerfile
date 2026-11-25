# Site Pet Soccer Online (Next.js, sortie « standalone ») : voir docker-compose.yml
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production NEXT_TELEMETRY_DISABLED=1 PORT=8080 HOSTNAME=0.0.0.0
RUN addgroup -S petsoccer && adduser -S -G petsoccer petsoccer
COPY --from=build --chown=petsoccer:petsoccer /app/.next/standalone ./
COPY --from=build --chown=petsoccer:petsoccer /app/.next/static ./.next/static
COPY --from=build --chown=petsoccer:petsoccer /app/public ./public
USER petsoccer
EXPOSE 8080
CMD ["node", "server.js"]
