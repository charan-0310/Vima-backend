# Docker Compose Profile Setup Guide

## Problem
The `application.properties` file had `spring.profiles.active=prod` hardcoded, which meant all builds (including dev) were using the prod profile.

## Solution
We've removed the hardcoded profile. Now you need to set the Spring profile via environment variables in your `docker-compose.yml` file.

## Required Changes to docker-compose.yml

Update your `docker-compose.yml` file on the EC2 server (`~/vima-app-deployment/docker-compose.yml`) to add the `SPRING_PROFILES_ACTIVE` environment variable to each service:

```yaml
version: "3.7"

services:
  nginx:
    image: nginx:1.28-alpine
    container_name: nginx
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/default.conf:/etc/nginx/conf.d/default.conf
      - ./data/certbot/conf:/etc/letsencrypt:ro
      - ./data/certbot/www:/var/www/certbot
    networks:
      - vima_net
      - wazuh-net
      - authentik-net

  certbot:
    image: certbot/certbot:latest
    container_name: certbot
    volumes:
      - ./data/certbot/conf:/etc/letsencrypt
      - ./data/certbot/www:/var/www/certbot

  vima-api:
    build:
      context: ./api
    container_name: vima-api
    restart: always
    environment:
      - JAVA_TOOL_OPTIONS=-XX:NativeMemoryTracking=summary -Xmx512m -Xms256m
      - SPRING_PROFILES_ACTIVE=prod  # ← ADD THIS LINE
    networks:
      - vima_net

  vima-api-dev:
    build:
      context: ./api-dev
    container_name: vima-api-dev
    restart: always
    environment:
      - JAVA_TOOL_OPTIONS=-XX:NativeMemoryTracking=summary -Xmx512m -Xms256m
      - SPRING_PROFILES_ACTIVE=dev  # ← ADD THIS LINE
    networks:
      - vima_net

networks:
  vima_net:
    driver: bridge
  wazuh-net:
    external: true
  authentik-net:
    external: true
```

## What Changed

### In the Code Repository:
- ✅ Removed hardcoded `spring.profiles.active=prod` from `application.properties`
- ✅ Added comment explaining profiles are set via environment variables

### On Your EC2 Server (Action Required):
- ⚠️ **You need to manually add** `SPRING_PROFILES_ACTIVE` environment variable to your `docker-compose.yml`:
  - `vima-api` service → `SPRING_PROFILES_ACTIVE=prod`
  - `vima-api-dev` service → `SPRING_PROFILES_ACTIVE=dev`

## How It Works

1. **Dev Deployment** (push to `develop` branch):
   - Builds JAR (no profile hardcoded)
   - Copies to `api-dev/artifact/`
   - Docker Compose sets `SPRING_PROFILES_ACTIVE=dev` → Uses `application-dev.properties`

2. **Prod Deployment** (push to `main` branch):
   - Builds JAR (no profile hardcoded)
   - Copies to `api/artifact/`
   - Docker Compose sets `SPRING_PROFILES_ACTIVE=prod` → Uses `application-prod.properties`

## Benefits

✅ **Complete Separation**: Dev and Prod use different configurations
✅ **No Hardcoding**: Profile is set at runtime, not build time
✅ **Flexible**: Easy to change profiles without rebuilding
✅ **Safe**: Each environment uses its own database, ports, and settings

## Verification

After updating docker-compose.yml, restart the services:

```bash
cd ~/vima-app-deployment
docker compose down
docker compose up -d --build
```

Check the logs to verify the correct profile is being used:

```bash
# Check dev service
docker logs vima-api-dev | grep "The following.*profile"

# Check prod service  
docker logs vima-api | grep "The following.*profile"
```

You should see:
- `vima-api-dev`: "The following 1 profile is active: dev"
- `vima-api`: "The following 1 profile is active: prod"
