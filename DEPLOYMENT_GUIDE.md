# Deployment Guide - Secure Blog REST API

This guide provides instructions for running the Secure Blog API locally using Docker and deploying to production environments.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development with Docker Compose](#local-development-with-docker-compose)
3. [Environment Variables](#environment-variables)
4. [Production Deployment](#production-deployment)
5. [CI/CD Pipeline](#cicd-pipeline)
6. [Monitoring & Health Checks](#monitoring--health-checks)
7. [Troubleshooting](#troubleshooting)
8. [Rollback Procedure](#rollback-procedure)

---

## Prerequisites

For local development:
- Docker Engine 20.10+ and Docker Compose 2.0+
- Git
- (Optional) MySQL client for direct database access

For production deployment:
- Linux server (Ubuntu 20.04+ or similar) with Docker & Docker Compose installed
- SSH access to the server
- Domain name configured to point to the server (optional but recommended)
- SSL certificate (Let's Encrypt or commercial)

---

## Local Development with Docker Compose

### Step 1: Clone and Setup

```bash
cd /path/to/secure-blog
```

### Step 2: Create Environment File

Create a `.env` file in the project root (see [Environment Variables](#environment-variables) section for details):

```bash
cp .env.example .env  # if .env.example exists
# Otherwise, create .env manually with the required variables
```

Required minimum:

```bash
# Database
DB_NAME=secure_blog
DB_USERNAME=blog_user
DB_PASSWORD=your-secure-password
DB_ROOT_PASSWORD=your-root-password

# JWT Secret (generate a strong secret, minimum 32 bytes)
APP_JWT_SECRET=$(openssl rand -base64 32)

# Spring Profile
SPRING_PROFILES=dev
```

### Step 3: Start Services

```bash
# Build and start both MySQL and backend
docker-compose up -d

# View logs
docker-compose logs -f backend
docker-compose logs -f mysql

# Check health
curl http://localhost:8080/api/v1/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "MySQL", "validationQuery": "isValid()" } },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### Step 4: Access the API

- Base URL: `http://localhost:8080/api/v1`
- Health endpoint: `http://localhost:8080/api/v1/actuator/health`
- Swagger/OpenAPI (if enabled): Not currently enabled in production profile

### Step 5: Stop and Clean Up

```bash
# Stop services
docker-compose down

# Stop and remove volumes (deletes database data)
docker-compose down -v
```

---

## Environment Variables

All secrets and configuration MUST be externalized via environment variables. Never commit secrets to Git.

### Required Variables

| Variable | Description | Example | Required For |
|----------|-------------|---------|--------------|
| `DB_NAME` | MySQL database name | `secure_blog` | All environments |
| `DB_USERNAME` | Database user (with CREATE/ALTER/DROP privileges) | `blog_user` | All environments |
| `DB_PASSWORD` | Password for `DB_USERNAME` | `S3cureP@ss!` | All environments |
| `DB_ROOT_PASSWORD` | MySQL root password (only for docker-compose) | `R00tP@ss!` | Local/dev only |
| `APP_JWT_SECRET` | JWT signing secret (minimum 256-bit / 32 bytes, base64-encoded) | `(base64 32+ bytes)` | All environments |
| `SPRING_PROFILES` | Spring profile (`dev`, `staging`, `prod`) | `prod` | All environments |

### Optional Variables

These are set in `application.properties` but can be overridden:

- `SERVER_PORT` (default: `8080`)
- `DB_HOST` (default: `mysql` in docker-compose, actual DB host in prod)
- `DB_PORT` (default: `3306`)

### Generating a Secure JWT Secret

```bash
# Generate a 256-bit (32-byte) random secret and base64-encode it
openssl rand -base64 32
# Output example: k3J9vN8sF2aP5wQ7xY4rT1uW6zA9bC0dE2fG3hI4jK5lM6nO7pQ8rS9tU0vW

# For production, use at least 512-bit (64 bytes):
openssl rand -base64 64
```

---

## Production Deployment

### Option 1: Docker Compose on a Single Server

#### 1. Prepare the Server

SSH into your production server:

```bash
ssh user@your-server.com
```

Install Docker and Docker Compose (if not already installed):

```bash
# Using official Docker installation script
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
newgrp docker

# Install Docker Compose plugin
sudo apt-get update
sudo apt-get install docker-compose-plugin
```

#### 2. Deploy Application

```bash
# Create deployment directory
sudo mkdir -p /opt/deployments/secure-blog
cd /opt/deployments/secure-blog

# Copy docker-compose.yml and .env file from your local machine
# Use scp or rsync:
# scp docker-compose.yml user@your-server.com:/opt/deployments/secure-blog/
# scp .env user@your-server.com:/opt/deployments/secure-blog/

# Create .env file with production values (DO NOT use dev secrets!)
nano .env

# Pull images and start services
docker-compose pull
docker-compose up -d

# Check logs
docker-compose logs -f backend

# Verify health
curl http://localhost:8080/api/v1/actuator/health
```

#### 3. Set Up Reverse Proxy (Nginx)

Install Nginx:

```bash
sudo apt-get install nginx
```

Create Nginx config `/etc/nginx/sites-available/secure-blog`:

```nginx
upstream backend {
    server localhost:8080;
}

server {
    listen 80;
    server_name api.yourdomain.com;

    location / {
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable and restart:

```bash
sudo ln -s /etc/nginx/sites-available/secure-blog /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

#### 4. Enable HTTPS with Let's Encrypt

```bash
sudo apt-get install certbot python3-certbot-nginx
sudo certbot --nginx -d api.yourdomain.com
```

Auto-renewal is configured automatically.

---

### Option 2: GitHub Actions Automated Deployment (Staging & Production)

The project includes a CI/CD pipeline that:

1. Runs tests on every push
2. Builds and pushes Docker image to GitHub Container Registry (GHCR)
3. Scans for vulnerabilities with Trivy
4. Auto-deploys to staging when `main` branch is pushed
5. Allows manual production deployment via workflow dispatch

#### Required GitHub Secrets

Add these secrets to your GitHub repository settings (`Settings > Secrets and variables > Actions`):

| Secret | Description | Where Used |
|--------|-------------|------------|
| `STAGING_HOST` | Staging server IP/hostname | deploy-staging job |
| `STAGING_USER` | SSH username for staging | deploy-staging job |
| `SSH_PRIVATE_KEY` | SSH private key (no passphrase) for accessing servers | All SSH deploy jobs |
| `STAGING_URL` | Full URL to staging API (e.g., `https://staging.api.yourdomain.com`) | Smoke tests |
| `PROD_HOST` | Production server IP/hostname | deploy-prod job |
| `PROD_USER` | SSH username for production | deploy-prod job |
| `PROD_URL` | Full URL to production API | Smoke tests |
| `SLACK_WEBHOOK` | Slack webhook URL for notifications | Notification step |
| `DB_NAME` | Database name (production) | SSH deploy script |
| `DB_USERNAME` | Database username (production) | SSH deploy script |
| `DB_PASSWORD` | Database password (production) | SSH deploy script |
| `APP_JWT_SECRET` | JWT secret for production | SSH deploy script |

**Note:** The database secrets (`DB_*`, `APP_JWT_SECRET`) are passed to the SSH script to generate the `.env` file on the target server. They are stored as GitHub Secrets, not hardcoded in the workflow.

#### Triggering Production Deployment

1. Merge your changes to `main` branch (auto-deploys to staging)
2. Verify staging deployment is healthy
3. Go to GitHub Actions tab → Select "Deploy to Production" workflow → Click "Run workflow"
4. The workflow will deploy the same image from staging to production

---

## Monitoring & Health Checks

### Health Endpoint

- **Local:** `GET http://localhost:8080/api/v1/actuator/health`
- **Production:** `GET https://api.yourdomain.com/api/v1/actuator/health`

Returns overall service health including database connectivity.

### Metrics (if actuator metrics enabled)

- `GET /api/v1/actuator/metrics` - List available metrics
- `GET /api/v1/actuator/metrics/jvm.memory.used` - Specific metric

### Logs

View logs with Docker Compose:

```bash
docker-compose logs -f backend
```

Or with plain Docker:

```bash
docker logs -f secure-blog_backend
```

Logs are stored in JSON format and rotated automatically. For production, consider shipping logs to a centralized system (CloudWatch, Loki, Papertrail).

---

## Troubleshooting

### Backend fails to start: "Unable to acquire JDBC Connection"

**Cause:** MySQL is not ready or credentials are wrong.

**Fix:**
```bash
# Check MySQL container is healthy
docker-compose ps mysql

# Check credentials in .env match application expectations
docker-compose logs mysql

# Test database connectivity from backend container
docker-compose exec backend sh
apk add --no-cache mysql-client  # if alpine
mysql -h mysql -u blog_user -p
```

### "APP_JWT_SECRET not set" error

**Fix:** Ensure `APP_JWT_SECRET` is set in `.env` or GitHub Secrets. The application will not start without it in production profile.

### Health endpoint returns 503 or hangs

**Cause:** Application still starting up, or database schema not initialized.

**Fix:**
```bash
# Check application logs for errors
docker-compose logs backend

# Verify database is accessible
docker-compose exec backend curl -v http://localhost:8080/api/v1/actuator/health

# Check if Flyway/Framework ran migrations (look for schema creation in logs)
```

### Docker image build fails: "Cannot resolve dependencies"

**Cause:** Maven cannot download dependencies (network issue).

**Fix:** The Dockerfile uses Maven's dependency cache. Clear local Docker build cache:
```bash
docker builder prune
docker-compose build --no-cache backend
```

---

## Rollback Procedure

### Docker Compose Rollback

If the new version breaks, roll back to the previous image:

```bash
cd /opt/deployments/secure-blog

# List images
docker images | grep secure-blog

# Pull previous version (replace with your previous tag or SHA)
docker pull ghcr.io/yourusername/secure-blog-backend:previous-sha

# Update docker-compose.yml image tag or use:
docker-compose up -d --no-deps --force-recreate backend
```

### GitHub Actions Rollback

1. Go to the GitHub Actions tab
2. Find the failed production deployment run
3. Click "Rerun" or manually deploy an older image by:
   - Creating a new branch with an old commit
   - Running the workflow manually with that commit
   - Or SSH into the server and use `docker-compose` to pull the previous image tag

---

## Backup and Restore

### Database Backup (MySQL)

```bash
# Backup
docker-compose exec mysql mysqldump -u root -p"$DB_ROOT_PASSWORD" $DB_NAME > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore
docker-compose exec -T mysql mysql -u root -p"$DB_ROOT_PASSWORD" $DB_NAME < backup_20240331_1200.sql
```

For production, set up automated daily backups using cron:

```bash
# crontab -e
0 2 * * * /usr/bin/docker exec secure-blog_mysql mysqldump -u root -p"$DB_ROOT_PASSWORD" secure_blog > /backups/secure-blog-$(date +\%Y\%m\%d).sql
```

---

## Security Checklist

- [ ] All secrets stored as environment variables, not in code
- [ ] `APP_JWT_SECRET` is at least 256-bit (32 bytes) random string
- [ ] Database passwords are strong and unique
- [ ] `SPRING_PROFILES_ACTIVE=prod` in production (disables dev endpoints)
- [ ] SSL/TLS enabled (HTTPS via Nginx + Let's Encrypt)
- [ ] Docker containers run as non-root user (already configured in Dockerfile)
- [ ] Regular security updates: `docker-compose pull` monthly at minimum
- [ ] Access logs enabled and monitored
- [ ] Database backups scheduled and tested

---

## Support & Contact

For issues with deployment:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review application logs: `docker-compose logs backend`
3. Check GitHub Actions workflow run logs
4. Open an issue in the repository

---

## Appendix: Docker Commands Reference

| Task | Command |
|------|---------|
| Build images | `docker-compose build` |
| Start services | `docker-compose up -d` |
| Stop services | `docker-compose down` |
| View logs | `docker-compose logs -f` |
| Execute command in container | `docker-compose exec backend sh` |
| List containers | `docker-compose ps` |
| Remove everything | `docker-compose down -v --rmi all` |

---

**Last Updated:** 2026-03-31
**Version:** 1.0.0
**Project:** Secure Blog REST API
