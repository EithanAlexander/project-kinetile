# Build static assets, then serve with nginx (proxies /api/v1/* to physics-engine).
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
# Same-origin /api/v1 via nginx; leave API base empty.
ENV VITE_PHYSICS_API_BASE_URL=
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
