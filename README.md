# Recipe Service

A Spring Boot microservice that ingests editorially recorded recipes (Figure 1
of the qualification task) and persists them into a Neo4j-backed ontology.

```
HTTP  ──►  Controller  ──►  DTO (+ Bean Validation)  ──►  Mapper  ──►
Service (@Transactional)  ──►  RecipeRepository (port)  ──►  Neo4j adapter
```

- **Java** 21 · **Spring Boot** 3.3.x · **Maven**
- **Neo4j** via Spring Data Neo4j
- **MapStruct** for DTO ↔ domain mapping
- **JUnit 5 + Mockito + Testcontainers** for tests
- **GitLab CI** publishing snapshots/releases to Artifactory

---

## Quick start

```bash
# 1. Start Neo4j (only needed once; data survives restarts)
docker run -d --name miele-neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/test12345 \
  neo4j:5.20

# 2. Build & run
mvn spring-boot:run

# 3. Send the Figure 1 payload
curl -i -X POST http://localhost:8080/api/v1/recipes \
  -H "Content-Type: application/json" \
  --data-binary @recipe.json

# 4. Inspect the graph at http://localhost:7474
#    MATCH (r:Recipe)-[:HAS_STEP]->(s) RETURN r, s;
```

Swagger UI: <http://localhost:8080/swagger-ui.html>
Actuator health: <http://localhost:8080/actuator/health>

---

## Endpoints

| Method | Path                       | Purpose                          |
|--------|----------------------------|----------------------------------|
| POST   | `/api/v1/recipes`          | Ingest a recipe                  |
| GET    | `/api/v1/recipes`          | List all stored recipes          |
| GET    | `/api/v1/recipes/{id}`     | Retrieve a single recipe         |

---

# Answers to the qualification questions

## a) Backend components for storing the recipe

The service is built as a clean, layered pipeline. Each concern lives in
exactly one place; cross-cutting concerns are handled centrally.

| Layer / concern             | Class                                   | Responsibility                                                      |
|-----------------------------|-----------------------------------------|---------------------------------------------------------------------|
| HTTP routing                | `RecipeController`                      | Accept JSON, return `201 Created` with a `Location` header.        |
| Input validation            | Bean Validation on `RecipeDTO`, `RecipeStepDTO`, `LabelDTO` | Declarative, runs before the service is invoked. |
| Wire format (write)         | `RecipeDTO`, `RecipeStepDTO`, `LabelDTO` | Mirrors the JSON contract; isolated from the domain model.        |
| Wire format (read)          | `RecipeResponse`                        | Read-side DTO; lets reads and writes evolve independently.         |
| DTO ↔ domain translation    | `RecipeMapper` (MapStruct-generated)    | Single, type-safe translation point; no reflection at runtime.     |
| Business orchestration      | `RecipeService`                         | `@Transactional`; rejects unsupported versions; entry point for future enrichment (ingredients, units, inference). |
| Domain / ontology entities  | `Recipe`, `RecipeStep`                  | `@Node`-annotated, modelled as a graph aggregate connected by a `HAS_STEP` relationship. |
| Persistence port            | `RecipeRepository`                      | Domain-facing interface; the service depends only on this.         |
| Persistence adapter         | `RecipeRepositoryAdapter`               | Thin bridge to the chosen storage technology.                      |
| Storage driver              | `Neo4jRecipeRepository` (Spring Data Neo4j) | Auto-implemented CRUD; the only place that *could* hold Cypher. |
| Cross-cutting errors        | `GlobalExceptionHandler` (`@RestControllerAdvice`) | Validation → 400, unknown version → 422, not found → 404, otherwise 500. |
| API documentation           | `springdoc-openapi`                     | Swagger UI generated from DTOs and validation annotations.         |
| Observability               | Spring Boot Actuator                    | `/actuator/health` for liveness/readiness, metrics endpoints.       |
| API versioning              | URL prefix `/api/v1/...`                | Allows breaking changes to live alongside the existing contract.   |

The request flow is:

```
POST /api/v1/recipes
   │
   ▼  @Valid triggers Bean Validation
RecipeController
   │
   ▼
RecipeService.ingest()   (@Transactional)
   │   1. version check
   │   2. mapper.toDomain(dto)  → Recipe + RecipeStep aggregate
   │   3. (future) enrichment / inference hooks
   ▼
RecipeRepository.save()   (interface – no Cypher leaks here)
   │
   ▼
Neo4jRecipeRepository   → Neo4j graph
```

---

## b) Advantages of object-oriented, encapsulated DB access over native queries

The persistence layer is intentionally split into **port + adapter + driver**
(`RecipeRepository`, `RecipeRepositoryAdapter`, `Neo4jRecipeRepository`). The
service never imports anything from Neo4j and never sees a line of Cypher.

Concrete advantages:

1. **Decoupling from the storage technology.**
   If Neo4j is replaced with another graph store or an RDF triple store
   (RDF4J, Jena, GraphDB, …), only the adapter changes. The service, the
   controller, the DTOs and the tests stay untouched.

2. **Type safety and refactorability.**
   With Spring Data, the repository methods are real Java methods. Renaming a
   field is a refactor the IDE can perform safely. With Cypher strings
   scattered across services, the same rename is a fragile search-and-replace
   that the compiler cannot help with.

3. **No SQL/Cypher injection by construction.**
   Spring Data binds parameters; hand-written query strings invite the
   classic injection pitfalls if anyone ever interpolates user input.

4. **Testability.**
   Because the service depends on the `RecipeRepository` interface, the unit
   test (`RecipeServiceTest`) simply mocks it with Mockito. No graph database
   is required to test business logic.

5. **Localized custom queries.**
   When a real custom Cypher query is genuinely needed, it lives in exactly
   one place (the Spring Data repository interface) instead of being
   duplicated as ad-hoc strings across services.

6. **Schema evolution is contained.**
   When the data model changes, the mapping/annotations and one repository
   change; consumers of the port do not.

7. **Transaction management is declarative.**
   `@Transactional` (read-only on reads, read-write on writes) wraps the
   whole aggregate write — recipe + steps + relationships — into one
   atomic unit. With native queries this discipline is easy to lose.

8. **Domain-driven naming.**
   `recipeRepository.save(recipe)` reads as the business operation it is,
   not as a graph traversal. The ontology lives in classes, the storage
   lives behind an interface.

The trade-off, honestly stated: for very complex graph traversals, hand-tuned
Cypher can outperform a generic repository call. The right strategy is
*encapsulation by default, raw query when measurably necessary* — and even
then the raw query lives behind the same interface.

---

## c) Problems that arise when the recipe data structure changes

A schema change has effects in at least five places, and each one has to be
considered explicitly. The project demonstrates concrete mitigations for
each.

### Categories of change

| Change | Impact | Mitigation already in the code |
|---|---|---|
| **New optional field** in the JSON | Old service rejects unknown JSON properties → 400. | `@JsonIgnoreProperties(ignoreUnknown = true)` on `RecipeDTO` (tolerant reader). |
| **Renamed JSON field** | Existing clients send the old name and break. | `@JsonAlias({"sequence","order","position"})` on `RecipeStepDTO.sequence`. |
| **New language for labels** | Hard-coded language fields would require a code change. | `LabelDTO` uses `@JsonAnySetter` / `@JsonAnyGetter` → any ISO-639-1 code works. |
| **Structurally different payload** (nested objects, removed fields, breaking change) | All clients break at once. | Explicit `version` field on the wire; URL prefix `/api/v1/...` so a `/api/v2` controller can live in parallel. |
| **Already-persisted data uses old shape** | New code reads old nodes incorrectly. | Each recipe persists `schemaVersion`; a migration job can iterate and rewrite, or the service can dual-read. |

### Concrete concerns to discuss

1. **Backwards compatibility of the API.** Existing clients (editorial UI,
   ingestion pipelines, mobile apps) cannot be upgraded in lock-step with the
   backend. We mitigate via tolerant readers, JSON aliases, the explicit
   `version` field, and the `/api/v1` namespace.

2. **Data already in the graph.** Even after the code changes, the database
   still contains nodes written under the old schema. Options:
   - **Dual-read**: branch in the mapper or service on `schemaVersion`.
   - **Lazy migration**: rewrite a node the first time it is touched.
   - **Eager migration job**: a one-shot Cypher migration with `APOC` or a
     Spring Batch job, recorded in `RELEASING.md`.

3. **Ontology consistency.** Recipes are not isolated rows — they are
   connected to ingredients, techniques, appliances, and so on. A renamed
   relationship type breaks every downstream consumer (inference engine,
   recommendation service, search). Schema changes therefore need a
   coordinated ontology change, not just a code change.

4. **Inference / derived data.** If the ontology drives an inference engine,
   migrating nodes is not enough — derived facts have to be re-computed
   under the new schema.

5. **Versioning everywhere.** Three independent version concepts must not be
   confused:
   - the **API version** (`/api/v1`),
   - the **payload schema version** (`RecipeDTO.version` / `Recipe.schemaVersion`),
   - the **artifact version** (Maven coordinate; see question d).

6. **Tests as a contract.** A regression test for *each historical wire
   format* protects against accidental breakage. The current test suite
   covers v1; a future v2 must add v1-compatibility tests, not replace
   them.

7. **Single change point.** Because all DTO ↔ domain translation happens in
   `RecipeMapper`, the cost of a schema change is bounded — exactly the
   point of having that layer.

8. **Communication.** Breaking API or wire-format changes require a
   coordinated release: changelog, advance notice to consumers, deprecation
   window, and ideally feature flags.

---

## d) Publishing the Maven artifact

The artifact `com.miele.ckb:recipe-service` is published via `mvn deploy`.
Key things to pay attention to:

- **Semantic versioning + `-SNAPSHOT`** — MAJOR.MINOR.PATCH; development versions carry `-SNAPSHOT`, release versions don't.
- **Snapshots vs. releases, and immutability** — `<distributionManagement>` points snapshots and releases to different Artifactory repos. A release coordinate is published **once** and never overwritten. The pipeline enforces this: only Git tags `vX.Y.Z` trigger a release deploy.
- **Centralized, reproducible versions** — Spring Boot's parent BOM plus an imported Testcontainers BOM in `<dependencyManagement>` give one version per dependency across the tree. No version ranges; every dependency is pinned. Plugin versions inherited from the parent.
- **Correct scopes** — `provided` for Lombok, `test` for Spring Boot Test / JUnit / Mockito / Testcontainers. Without correct scopes, consumers would inherit Testcontainers and start Docker accidentally.
- **Dependency hygiene** — `mvn dependency:tree`, `mvn dependency:analyze`, `mvn versions:display-dependency-updates` are run before tagging a release; warnings are addressed.
- **Credentials never in the repo** — `.gitlab/settings.xml` resolves `${env.ARTIFACTORY_USER}` / `${env.ARTIFACTORY_PASSWORD}` from masked, protected GitLab CI variables.
- **Pipeline-driven publishing** — `.gitlab-ci.yml` runs build → test (unit + Testcontainers IT) → deploy. `deploy:snapshot` runs on the default branch; `deploy:release` runs only on `vX.Y.Z` tags, sets the project version from the tag, and uploads to the releases repo. Humans never run `mvn deploy` manually against the corporate Artifactory.
- **Communicating changes** — `CHANGELOG.md` updated each release; MAJOR bumps include a "Breaking changes" section; `RELEASING.md` documents the cut-a-release process.