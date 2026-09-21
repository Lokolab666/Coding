---
label: Containerizing Your Application
icon: container
order: 30
---

# Containerizing Your Application

Use this guide to build smaller, safer, and faster containers.

---

## What is a Container?

A container is a lightweight, standalone package that includes everything needed to run your application: code, runtime, system tools, libraries, and settings. Containers isolate your application from the underlying infrastructure, ensuring it runs consistently across development, testing, and production environments.

**Key benefits:**
- **Consistency** - "Works on my machine" becomes "works everywhere"
- **Portability** - Run the same container on your laptop, in CI/CD, and in production
- **Isolation** - Each container runs independently without conflicting with other applications
- **Efficiency** - Containers share the host OS kernel, making them lighter and faster than virtual machines

**How containers work with Compass CI:**
1. You define a `Dockerfile` that describes how to build your application
2. The CI pipeline builds your Dockerfile into a versioned container image
3. Each image version is stored in Artifactory (Medtronic's artifact repository)
4. Kubernetes pulls the image and runs it as a container in the cluster

Think of a container image as a snapshot of your application and its dependencies, and a running container as an instance of that snapshot.

---

## Quick Checklist

- Use multi-stage builds to separate build and runtime layers
- Prefer pinned image tags (avoid `latest`)
- Use Artifactory-backed images and package registries
- Add a `.dockerignore` file to your project root to reduce context size
- Run as a non-root user
- Include a health check endpoint
- Keep runtime images minimal (no compilers, package managers, or build tools)

---

## Recommended Dockerfile Structure

### Node Example
```Dockerfile
# Build stage
FROM case.artifacts.medtronic.com/ext-docker-hub-remote/node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Runtime stage
FROM case.artifacts.medtronic.com/ext-docker-hub-remote/node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules

# Run as non-root
USER 10001

EXPOSE 8080
CMD ["node", "dist/index.js"]
```

### Java Example (JDK build, JRE runtime)

```Dockerfile
# Build stage (JDK)
FROM case.artifacts.medtronic.com/ext-docker-hub-remote/gradle:jdk17-alpine AS builder
WORKDIR /workspace
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN ./gradlew --no-daemon clean build -x test
COPY src/ src/
RUN ./gradlew --no-daemon clean build -x test

# Runtime stage (JRE only)
FROM case.artifacts.medtronic.com/ext-docker-hub-remote/eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

# Run as non-root
USER 10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Notes:**
- Build tools and caches stay in the builder image
- Runtime image contains only the JRE and your app
- Use `-x test` only if tests run earlier in CI

**Why this works well:**
- Build dependencies stay in the builder image
- The runtime image is smaller and easier to scan
- Layering keeps rebuilds fast when only app code changes

---

## Base Images and Builder Images

### Use Artifactory-backed Images

Pull images through Medtronic Artifactory to avoid network restrictions on shared runners and to improve cache reliability:

For a full list of external repositories, see https://documentation.shared-services-prd.eks.mdtcloud.io/cicd-platform/jfrog/external-repositories/

```
case.artifacts.medtronic.com/ext-docker-hub-remote/<image>:<tag>
```

Example:
```Dockerfile
FROM case.artifacts.medtronic.com/ext-docker-hub-remote/node:20-alpine AS builder
```

### Builder Images

Use builder images for build stages only (Node, Maven, Gradle, etc.), and keep runtime images minimal. If your build needs a specialized image, publish it to Artifactory and use it as the builder image.

---

## Package Access via Artifactory

GitLab runners restrict outbound access. Use Artifactory-backed registries to avoid timeouts.

### Alpine Packages

Add a repository file and overwrite the default repositories:

```properties
https://case.artifacts.medtronic.com/artifactory/ext-alpine-cdn-remote/v3.20/main
@community https://case.artifacts.medtronic.com/artifactory/ext-alpine-cdn-remote/v3.20/community
@edge https://case.artifacts.medtronic.com/artifactory/ext-alpine-cdn-remote/edge/main
@edge-testing https://case.artifacts.medtronic.com/artifactory/ext-alpine-cdn-remote/edge/testing
```

Dockerfile usage:
```Dockerfile
ADD mdt-alpine-repositories.txt /etc/apk/repositories
RUN dos2unix /etc/apk/repositories
```

### npm

`npmrc` file:
```properties
registry=https://case.artifacts.medtronic.com/artifactory/api/npm/int-npm-virtual/
```

Dockerfile usage:
```Dockerfile
ADD npmrc /usr/local/etc/npmrc
```

### Pip

`pip.conf` file:
```properties
[global]
index-url = https://case.artifacts.medtronic.com/artifactory/api/pypi/ext-pypi-python-remote/simple
```

Dockerfile usage:
```Dockerfile
COPY pip.conf /etc/pip.conf
```

### Maven or Gradle

Point your build tool to Artifactory-hosted repos:

```gradle
repositories {
    maven {
        url "https://case.artifacts.medtronic.com/artifactory/bcp-pub-frameworks-virtual"
    }
}
```

```xml
<repositories>
  <repository>
    <id>central</id>
    <url>https://case.artifacts.medtronic.com/artifactory/bcp-pub-frameworks-virtual</url>
  </repository>
</repositories>
```

---

## Layering for Faster Builds

Order layers so the most stable steps are first:

1. Copy package manager files (`package.json`, `package-lock.json`, `pom.xml`)
2. Install dependencies
3. Copy application source
4. Build

This keeps caches warm between builds and avoids reinstalling dependencies on every change.

---

## Security and Hardening

- Run as non-root (`USER 10001` or similar)
- Avoid `latest` tags
- Remove build tools from runtime images
- Keep runtime images minimal
- Use explicit health checks

---

## Running Your Application Locally

### Install Docker Desktop

Download and install [Docker Desktop](https://www.docker.com/products/docker-desktop/) to build and run containers on your local machine. Docker Desktop includes Docker Engine, Docker CLI, and Docker Compose.

### Build and Run a Single Container

Build your Dockerfile locally:

```bash
docker build -t my-app:local -f Dockerfile .
```

Run the container:
```bash
docker run --rm -it -p 8080:8080 my-app:local
```

**Flags explained:**
- `--rm` - Remove container when it stops
- `-it` - Interactive terminal
- `-p 8080:8080` - Map host port 8080 to container port 8080

**Pass environment variables:**
```bash
docker run --rm -it -p 8080:8080 \
  -e DATABASE_URL=jdbc:oracle:thin:@//localhost:1521/FREEPDB1 \
  -e LOG_LEVEL=debug \
  my-app:local
```

**Or use an env file:**
```bash
docker run --rm -it -p 8080:8080 --env-file=local.env my-app:local
```

`local.env`:
```properties
DATABASE_URL=jdbc:oracle:thin:@//localhost:1521/FREEPDB1
LOG_LEVEL=debug
API_KEY=your-key-here
```

### Multi-Container Setup with Docker Compose

For applications that depend on databases, caches, or other services, use Docker Compose to run everything together.

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  app:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: jdbc:oracle:thin:@//db:1521/FREEPDB1
      REDIS_URL: redis://redis:6379
    depends_on:
      - db
      - redis
    volumes:
      - ./src:/app/src  # Hot reload during development

  db:
    image: case.artifacts.medtronic.com/ext-docker-hub-remote/gvenzl/oracle-free:23-slim
    environment:
      ORACLE_PASSWORD: password
      APP_USER: appuser
      APP_USER_PASSWORD: appuserpassword
    ports:
      - "1521:1521"
    volumes:
      - oracle_data:/opt/oracle/oradata

  redis:
    image: case.artifacts.medtronic.com/ext-docker-hub-remote/redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  oracle_data:
```

**Start all services:**
```bash
docker-compose up
```

**Access your application in a browser:**

Once the containers are running, open your browser and navigate to:
```
http://localhost:8080
```

The port number (`8080` in this example) comes from the port mapping in your `docker-compose.yml`:
```yaml
ports:
  - "8080:8080"  # host:container
```

**Understanding port mapping:**
- **Left side (host):** Port on your local machine → `localhost:8080`
- **Right side (container):** Port inside the container → `8080`

**If you used a different host port:**
```yaml
ports:
  - "3000:8080"  # Maps container port 8080 to host port 3000
```
Access at: `http://localhost:3000`

**Common ports:**
- Web apps: `3000`, `8080`, `8000`
- APIs: `8080`, `3000`, `5000`
- Databases: `12021` (Oracle), `3306` (MySQL), `6379` (Redis)

**Tip:** Use Docker Desktop's dashboard to see running containers and click the port link to open in your browser automatically.

**Run in background:**
```bash
docker-compose up -d
```

**View logs:**
```bash
docker-compose logs -f app
```

**Stop all services:**
```bash
docker-compose down
```

**Rebuild after code changes:**
```bash
docker-compose up --build
```

### Development Workflow Tips

**Hot reload during development:**

Mount your source code as a volume so changes can reflect without rebuilding (works best for interpreted/dev-server workflows such as Node and Python):

```yaml
volumes:
  - ./src:/app/src
  - ./package.json:/app/package.json
```

For compiled runtimes (for example Java), mounting `./src` alone is not enough if your container runs a prebuilt jar. In that setup, rebuild after code changes:

```bash
docker-compose up --build
```

**Run commands inside a running container:**
```bash
docker exec -it <container-name> sh
docker-compose exec app sh
```

**Check container status:**
```bash
docker ps
docker-compose ps
```

**Verify environment variables:**
```bash
docker exec <container-name> env
docker-compose exec app env
```

---

## Troubleshooting Tips

- **Package downloads failing:** Verify Artifactory registries and repository files
- **Slow builds:** Check layer ordering and `.dockerignore`
- **Large images:** Confirm multi-stage build and remove build tools from runtime stage
- **OOM or CPU throttling:** Reduce build parallelism or increase build resources

---

## Related Guides

- [GitLab Repository Configuration](./gitlab-repository-configuration.md)
- [Kubernetes Manifests](./kubernetes-manifests.md)
