# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# Use Node to install dependencies and run the Vite build
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files first — cached layer, only re-runs if dependencies change
COPY package*.json .
RUN npm ci --quiet

# Copy source and build
# VITE_API_BASE_URL is injected at build time via --build-arg
ARG VITE_API_BASE_URL=http://localhost:9090/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY . .
RUN npm run build

# ─── Stage 2: Serve ───────────────────────────────────────────────────────────
# Use Nginx to serve the built static files — lightweight and production-ready
FROM nginx:alpine

# Remove default Nginx config and replace with ours
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from the build stage into Nginx's web root
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
