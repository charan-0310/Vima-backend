# Deployment Structure

This document describes the separated deployment structure for Dev and Prod environments.

## Directory Structure on EC2

```
/home/ubuntu/vima-app-deployment/
├── dev/
│   ├── api-dev/
│   │   └── artifact/
│   │       └── vimaadmin-0.0.1-SNAPSHOT.jar
│   └── docker-compose.dev.yml
└── prod/
    ├── api/
    │   └── artifact/
    │       └── vimaadmin-0.0.1-SNAPSHOT.jar
    └── docker-compose.prod.yml
```

## Workflow Configuration

### Dev Environment (`vima-dev` branch)
- **Workflow**: `.github/workflows/deploy_dev.yml`
- **JAR Location**: `~/vima-app-deployment/dev/api-dev/artifact/`
- **Docker Compose**: `docker-compose.dev.yml` in `~/vima-app-deployment/dev/`
- **Trigger**: Push to `vima-dev` branch

### Prod Environment (`main` branch)
- **Workflow**: `.github/workflows/main.yml`
- **JAR Location**: `~/vima-app-deployment/prod/api/artifact/`
- **Docker Compose**: `docker-compose.prod.yml` in `~/vima-app-deployment/prod/`
- **Trigger**: Push to `main` branch

## Benefits

1. **Complete Isolation**: Dev and Prod deployments don't interfere with each other
2. **Separate Configurations**: Each environment can have its own Docker Compose configuration
3. **Independent Restarts**: Restarting one environment doesn't affect the other
4. **Clear Separation**: Easy to identify which environment is which

## Setup Instructions

On your EC2 instance, create the directory structure:

```bash
mkdir -p ~/vima-app-deployment/dev/api-dev/artifact
mkdir -p ~/vima-app-deployment/prod/api/artifact
```

Then create your Docker Compose files:
- `~/vima-app-deployment/dev/docker-compose.dev.yml` (for dev)
- `~/vima-app-deployment/prod/docker-compose.prod.yml` (for prod)

## Migration Notes

If you have an existing deployment, you'll need to:

1. Move existing JAR files to the new structure
2. Create separate docker-compose files for dev and prod
3. Update any references to the old paths

The workflows will automatically create the directory structure if it doesn't exist.
