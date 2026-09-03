# Print/Render Service - Technical Interview

### Context

We run a rendering system. Clients submit a **render job** (a template id plus some parameters);
the job is processed **asynchronously**, and processing can occasionally fail transiently.
Clients poll for the job's status and fetch the result once it is done.

The set of available templates already exists (`RenderTemplate` / `GET /templates`) - you do not
need to build template management. Your job is to implement the render job lifecycle end to end,
**including how the service is packaged and run**.

### Prerequisites

- **JDK 25** installed locally (the Maven wrapper handles Maven itself, but you need a JDK to run
  `./mvnw` or your IDE).
- **Docker Desktop** (Mac/Windows) or **Docker Engine + the Compose plugin** (Linux), installed
  and running. Everything containerization-related in this exercise (the `Dockerfile` you're
  given, and the `docker-compose.yml` you write) is public/open-source - no account, license, or
  paid service is required. `docker compose version` should print a v2.x version.
- Ports **8080** (the app) and **5432** (Postgres) free on your machine, or be ready to remap them
  in your `docker-compose.yml` if something else is already using them.
- Git, to fork/push your solution.

### Functional Requirements

- **Submit a job**: `POST /jobs`
  - Body: `{ "templateId": "<uuid>", "parameters": { "any": "key-value data" } }`
  - Must return immediately (do not block the HTTP response on the actual rendering work).
  - Reject with `400` if `templateId` does not match an existing template.
  - On success, return `201` with the created job (id, status `QUEUED`, timestamps).

- **Process a job asynchronously**: once queued, a job must move through
  `QUEUED -> PROCESSING -> DONE` or `QUEUED -> PROCESSING -> FAILED`, driven by a background
  worker - not by an incoming HTTP request. "Rendering" can be simulated (e.g. a short delay);
  it does not need to produce a real document.
  - Some renders fail transiently. A failed attempt should be retried a bounded number of times
    before the job is marked `FAILED` with an error reason recorded.

- **Get job status**: `GET /jobs/{id}` - current status, attempt count, error message if failed,
  and whether a result is available.

- **List jobs**: `GET /jobs` - optionally filterable by status, e.g. `GET /jobs?status=FAILED`.

- **Fetch a result**: `GET /jobs/{id}/result` - returns the rendered output once the job is
  `DONE`. Decide yourself what should happen if it's called before the job finishes, or if the
  job failed.

### Required

- Your solution - code, commit messages, and README - must be in English.
- Java 25.
- This repo contains the existing project skeleton (template lookup, project setup, and a starter
  `Job` entity with the fields implied by the API contract above); fork or create a public
  repository with your solution. The `Job` entity does not model retry scheduling/backoff - that's
  part of what you're designing.
- You decide the scope of automated tests - we expect to see some.
  `RenderTemplateResourceTest` shows the MockMvc setup used in this repo, if that's a useful
  starting point.
- **The service must be containerized and runnable via Docker Compose alongside an actual
  PostgreSQL server** - i.e. a `postgres` Docker image running as its own container, not H2 or
  any other in-memory/embedded database. A `Dockerfile` for this service is provided; you need to
  write the `docker-compose.yml` that wires this app together with that `postgres` service (the
  app already reads its DB connection from `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` /
  `DB_PASSWORD`, see `application.properties`). H2 is fine for your own test suite - it must not
  be what the running application connects to.
- The service must expose:
  - A liveness endpoint.
  - A readiness endpoint that reflects more than "the HTTP server is up" - think about what else
    a caller would want to know before considering this service ready to take traffic.
  - Some form of basic metrics (job counts by status is enough; format is up to you).
  - Paths for all three are up to you - just document them in your README so we know where to
    look.
- **Add a short "Design Decisions" section to your README** covering the points listed below
  under "A few things we deliberately left open." A few sentences per point is enough - this is
  the starting point for the design discussion, not a design doc.
- **Once the code is complete, reply to your hiring contact with a link to your repository.**

### A few things we deliberately left open

We're not going to tell you how to implement the queue/worker, the retry policy, or what your
readiness check should verify - that's for you to decide.

### Design Decisions

#### Queue and worker

Jobs are stored in PostgreSQL and processed by a scheduled background worker. The worker polls
for eligible `QUEUED` jobs and atomically claims one before rendering it, so two application
instances sharing the database cannot claim the same job. Rendering happens after the claim and
never on the HTTP request thread; PostgreSQL is the durable source of truth for job state.

#### Retry policy

Only `TransientRenderingException` is retryable. A job receives up to three retries, with a 
30-second `nextAttemptAt` backoff between attempts.
When the retry budget is exhausted, or a non-transient error occurs, the job is marked `FAILED` and
the error reason is persisted. The renderer's delay and transient-failure rate are configurable so
the policy can be exercised locally without changing business logic.

### Readiness and liveness and metrics

`/health/live` only indicates that the process and HTTP server are responsive. `/health/ready`
performs a lightweight database query and returns `503 Service Unavailable` when PostgreSQL cannot
be reached, preventing traffic from being routed to an instance that cannot read or persist jobs.
`/metrics` returns a JSON object with job counts by status, e.g.:
```json
{
  "QUEUED": 3,
  "PROCESSING": 1,
  "DONE": 5,
  "FAILED": 2
}
```


### Optional (not required to complete the exercise)

- Demonstrate that running two instances of your app against the same database does not cause a
  job to be processed twice.
- A Kubernetes `Deployment`/`Service` manifest for this app (it does not need to be applied to a
  real cluster - we're interested in the manifest itself, e.g. how you wire up probes).

### How to run

Building
```shell
$ ./mvnw compile
```

Test
```shell
$ ./mvnw test
```

Start the application (once you've written `docker-compose.yml`)
```shell
$ docker compose up --build
```

Listing available templates
```shell
$ curl localhost:8080/templates
```
