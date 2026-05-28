# Recipe Service

Spring Boot microservice that ingests editorially recorded recipes (Figure 1
of the qualification task) and persists them into a Neo4j-backed ontology.

- **Java** 21 · **Spring Boot** 3.3 · **Maven**
- **Neo4j** via Spring Data Neo4j
- **MapStruct** for DTO ↔ domain mapping
- **JUnit 5 + Mockito + Testcontainers** for tests

## Quick start

```bash
# 1. Start Neo4j
docker run -d --name miele-neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/test12345 \
  neo4j:5.20

# 2. Run the service
mvn spring-boot:run

# 3. POST a recipe
curl -i -X POST http://localhost:8080/api/v1/recipes \
  -H "Content-Type: application/json" \
  --data-binary @recipe.json
```

Swagger UI: <http://localhost:8080/swagger-ui.html>

## Endpoints

| Method | Path                     | Purpose            |
|--------|--------------------------|--------------------|
| POST   | `/api/v1/recipes`        | Ingest a recipe    |
| GET    | `/api/v1/recipes`        | List all recipes   |
| GET    | `/api/v1/recipes/{id}`   | Retrieve by id     |

---

# Answers to the qualification questions

## a) Backend components

The service is a layered pipeline; each concern lives in exactly one place.

```
HTTP ─► Controller ─► DTO (@Valid) ─► Mapper ─► Service (@Transactional)
        ─► RecipeRepository (port) ─► Neo4j adapter ─► Neo4j
```

- **`RecipeController`** — HTTP routing, returns `201 Created` with a `Location` header.
- **`RecipeDTO` / `RecipeStepDTO` / `LabelDTO`** — wire format, validated declaratively via Jakarta Bean Validation.
- **`RecipeMapper`** (MapStruct) — single, generated, type-safe DTO ↔ domain translation.
- **`RecipeService`** — `@Transactional` business orchestration; checks the payload `version`; hook point for future ontology enrichment.
- **`Recipe` / `RecipeStep`** — domain entities modelled as `@Node`s connected by a `HAS_STEP` relationship.
- **`RecipeRepository`** (interface) + **`RecipeRepositoryAdapter`** + **`Neo4jRecipeRepository`** — port/adapter/driver split so the service never touches Neo4j directly.
- **`GlobalExceptionHandler`** — central `@RestControllerAdvice` mapping validation → 400, unknown version → 422, not found → 404.
- **Cross-cutting**: Actuator (`/actuator/health`), springdoc-openapi (Swagger UI), URL prefix `/api/v1` for API versioning.

## b) Encapsulated DB access vs. native queries

The service depends on the `RecipeRepository` **interface**, not on Neo4j. No
Cypher exists in the business code. Concrete advantages:

- **Swappable storage** — replacing Neo4j with another graph or RDF store changes one adapter class; the service, controller, DTOs and tests stay untouched.
- **Type safety and refactorability** — repository methods are real Java methods; renames are IDE-safe. Cypher strings scattered across services are not.
- **No injection by construction** — Spring Data binds parameters; hand-written query strings invite the classic injection pitfalls.
- **Testability** — `RecipeServiceTest` mocks the repository with Mockito; no database needed to test business logic.
- **Localized custom queries** — when a real custom query is genuinely needed, it lives in *one* repository interface, not duplicated as ad-hoc strings.
- **Declarative transactions** — `@Transactional` (read-only on reads) wraps the whole aggregate write atomically.
- **Domain-driven naming** — `repository.save(recipe)` reads as a business operation, not a graph traversal.

Honest trade-off: complex traversals can be faster with hand-tuned Cypher. The
right rule is *encapsulation by default, raw query when measurably necessary* —
and even then it lives behind the same interface.

## c) Adapting the data structure

A schema change touches the wire format, the persisted graph, the ontology,
derived/inferred data, and all clients. Concrete mitigations already in the
code:

| Change                    | Mitigation                                                              |
|---------------------------|-------------------------------------------------------------------------|
| New optional JSON field   | `@JsonIgnoreProperties(ignoreUnknown = true)` on `RecipeDTO` (tolerant reader). |
| Renamed JSON field        | `@JsonAlias({"sequence","order","position"})` on `RecipeStepDTO.sequence`. |
| New language for labels   | `@JsonAnySetter` / `@JsonAnyGetter` on `LabelDTO` — any ISO-639-1 code works without code changes. |
| Breaking structural change| Explicit `version` field on the wire + `/api/v1` URL prefix → a parallel `/api/v2` controller can coexist. |
| Old data already in the DB| Each node stores `schemaVersion`; the service can dual-read or a migration job rewrites old nodes. |

Things to keep in mind beyond the code:

- **Ontology consistency** — recipes link to ingredients, techniques, appliances. Renaming a relationship type breaks every downstream consumer (inference, search, recommendations). Schema changes are coordinated ontology changes, not just code changes.
- **Derived/inferred data** — if an inference engine produces facts, migrating nodes is not enough; derived data has to be re-computed.
- **Three independent version concepts** that must not be confused: the *API version* (`/api/v1`), the *payload schema version* (`version` field), and the *artifact version* (Maven coordinate).
- **Tests as a contract** — keep regression tests for every historical wire format; a future v2 must *add* v1-compatibility tests, not replace them.
- **Single change point** — because all DTO ↔ domain translation happens in `RecipeMapper`, the blast radius of a schema change is bounded.
- **Communication** — breaking changes require a changelog entry, advance notice to consumers, and a deprecation window.

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