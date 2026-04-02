# REST Assured Java API Framework

This project is a basic but runnable API automation framework built with Java, Maven, TestNG, and Rest Assured. It includes:

- A local embedded API server that creates and stores user data.
- Optional PostgreSQL-backed persistent storage for the user API.
- Token-based authentication with access tokens, refresh tokens, caching, and synchronized refresh handling.
- Simple and complex nested POJO examples for request/response modelling.
- A TestNG-based API automation layer.
- A configurable high-volume test path that can scale to thousands of API calls.
- Environment-specific config files for local, QA, stage, and prod execution.
- Docker assets for the API server and test runner.
- Kubernetes manifests for deploying the API and running tests as a job.

## Project Structure

```text
src/
  main/java/com/example/framework/server    -> local API server
  main/java/com/example/framework/model     -> shared request/response models
  test/java/com/example/framework           -> framework code and API tests
  test/resources                            -> framework config and endpoint paths
k8s/                                        -> Kubernetes manifests
```

The default mode still uses in-memory storage. Persistent storage is currently implemented for `/users` when the server is started with PostgreSQL settings.

## What The Local Server Exposes

- `GET /health`
- `POST /auth/token`
- `POST /auth/refresh`
- `POST /users`
- `GET /users`
- `GET /users/{id}`
- `PUT /users/{id}`
- `PATCH /users/{id}`
- `DELETE /users/{id}`
- `POST /seed?count=10`
- `POST /profiles`
- `PUT /profiles/{id}`
- `PATCH /profiles/{id}`
- `GET /profiles/{id}`
- `DELETE /profiles/{id}`
- `POST /orders`
- `PUT /orders/{id}`
- `PATCH /orders/{id}`
- `GET /orders/{id}`
- `DELETE /orders/{id}`

`PUT` expects a full replacement payload. `PATCH` supports partial updates, deep-merges nested objects, and replaces arrays when they are provided.

## Run Locally

Start the test suite with the embedded API server:

```bash
mvn test
```

Run with a higher request volume:

```bash
mvn test -Dapi.request.count=10000 -Dapi.parallel.threads=50
```

Run against a named environment:

```bash
mvn test -Dapi.env=qa
```

Enable full Rest Assured request and response logging:

```bash
mvn test -Dapi.logging.enabled=true
```

Change the logging detail level:

```bash
mvn test -Dapi.logging.enabled=true -Dapi.logging.detail=BODY
```

Available environment files:

```text
src/test/resources/config.properties
src/test/resources/config-local.properties
src/test/resources/config-qa.properties
src/test/resources/config-stage.properties
src/test/resources/config-prod.properties
src/test/resources/endpoints.properties
```

Logging-related properties:

```text
api.logging.enabled=false
api.logging.detail=ALL
api.logging.on.validation.failure=true
api.response.time.assert.enabled=true
api.response.time.max.ms=5000
```

Retry-related properties:

```text
api.retry.enabled=true
api.retry.max.attempts=3
api.retry.delay.ms=250
api.retry.retryable.status.codes=429,500,502,503,504
```

Example retry override:

```bash
mvn test -Dapi.retry.max.attempts=5 -Dapi.retry.delay.ms=500
```

Example response-time override:

```bash
mvn test -Dapi.response.time.max.ms=2000
```

Authentication-related properties:

```text
api.auth.enabled=true
api.auth.username=demo-api-user
api.auth.password=demo-api-password
api.auth.access.token.expiry.skew.seconds=3
```

Package the standalone API server jar:

```bash
mvn -DskipTests package
```

Run the API server directly:

```bash
java -Dserver.port=9876 -jar target/rest-api-framework-1.0.0-shaded.jar
```

Run the API server with PostgreSQL-backed user storage:

```bash
java -Dserver.port=9876 -Dserver.user.store.type=jdbc -Ddb.url=jdbc:postgresql://localhost:5432/api_framework -Ddb.username=postgres -Ddb.password=postgres -jar target/rest-api-framework-1.0.0-shaded.jar
```

Run tests against an already-running server:

```bash
mvn test -Dapi.base.url=http://localhost:9876 -Dapi.start.embedded.server=false
```

## Docker

Build the API image:

```bash
docker build -f Dockerfile.server -t java-api-framework/local-api:latest .
```

Build the test image:

```bash
docker build -f Dockerfile.tests -t java-api-framework/api-tests:latest .
```

Run both with Compose:

```bash
docker compose up --build --abort-on-container-exit
```

`docker compose` now starts PostgreSQL, the API server, and the test runner together. The API container uses JDBC-backed persistent storage for `/users`, while the nested profile and order demo endpoints still remain in memory.

The test layer also performs automatic per-test cleanup for created users, profiles, and orders. Each test registers the ids it creates, and `BaseApiTest` deletes them after the method finishes in reverse order. This keeps runs isolated, which is especially important when you use persistent storage or run inside Docker and Kubernetes.

Docker test reports are written to:

```text
docker-results/api-tests/
```

## Kubernetes

Apply the API deployment and service:

```bash
kubectl apply -f k8s/local-api-deployment.yaml
kubectl apply -f k8s/local-api-service.yaml
```

Run the test job:

```bash
kubectl apply -f k8s/api-tests-job.yaml
```

## Scale Notes

- The server uses `ConcurrentHashMap` and `AtomicLong`, so it is safe for concurrent local test traffic.
- The test suite uses TestNG parallel execution and a parallel data provider.
- For 10,000 calls, increase JVM heap and tune `api.parallel.threads` based on your machine or cluster capacity.
- In Kubernetes, scale the API deployment replicas separately from the test job parallelism.
- Docker-based test runs write reports into `docker-results/api-tests` on the host machine.







