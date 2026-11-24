# AGENTS.md — quarkus-lambda-http-template

HTTP API on AWS Lambda (Quarkus native, behind an API Gateway HTTP API) that reads and writes a MySQL database.
`Item` is the example resource of the archetype: replace it with the resources of the service (see `docs/api/item/README.md`).

## OpenAPI-first workflow

1. Edit `src/main/resources/openapi.yaml` first (single source of truth). Every JSON property, query
   parameter and path segment is snake_case/kebab-case
   - A field used by several requests is a component (e.g. `ItemName`) referenced from each one, so its pattern is defined once
   - `required` goes in the same object as its `properties`, without `allOf`: openapi-typescript (frontend) ignores a `required` that is in another `allOf` block
   - A null field is not written in the response (`non_null`, set by quarkus-lambda-commons-http): a response field that can be missing is optional, out of `required` and without `nullable: true`
   - A response field that is always sent goes in `required` (e.g. `id`), so the frontend types don't make it optional
   - The path of `servers[0].url` (`/api/v1`) is the same as `quarkus.http.root-path` in `application.yaml`, the only one that sets it: the generator doesn't create an `@ApplicationPath` (`generateRootResources=false` in the parent)
2. Run codegen: `./mvnw generate-sources` — generates DTOs + JAX-RS interfaces into `target/generated-sources/openapi/src/main/java/com/example/api/`
3. Implement the generated interfaces in `com.example.app.api.*Impl`
4. Create a Flyway migration in `src/main/resources/flyway/` if the schema changes

Generated DTOs and interfaces are NOT committed (under `target/`). IDE must include generated sources.

## Commands

| Action | Command |
|---|---|
| Run all tests | `./mvnw clean test` |
| Single test class | `./mvnw test -Dtest=HealthResourceTest` |
| Codegen only | `./mvnw generate-sources` |
| Native build | `./mvnw package -Pnative -DskipTests` |
| Docker native build | `docker build -t app .` |

No linter/formatter enforced — PR workflow tries `spotless:check || checkstyle:check` but tolerates absence.

## Architecture

```
com.example.app
├── api/              # *ResourceImpl implements generated *Resource, MapStruct *ResourceMapper
├── business/
│   ├── entity/       # JPA @Entity, one per table
│   ├── repository/   # Interfaces
│   └── service/      # @ApplicationScoped business logic, one service per endpoint
└── infrastructure/
    └── mysql/        # EntityManager-based implementations, annotated @XRayTraced
```

The code every HTTP lambda repeats comes from `com.xdyan:quarkus-lambda-commons-http`
([quarkus-lambda-platform](https://github.com/User-Jean/quarkus-lambda-platform)); its beans and providers are active
just by being in the pom, nothing is declared here:
- `com.xdyan.commons.xray`: `@XRayTraced` and its interceptor (X-Ray subsegments), `ClientError`
- `com.xdyan.commons.clock`: the `Clock` bean, in the zone `xdyan.commons.clock.zone` (`application.yaml`)
- `com.xdyan.commons.http.error`: `ErrorMessage` and the providers for 400/404/409/500, `ConflictException` (409),
  `InvalidFieldsException` (400 with one error per field). Each provider logs the exception: 4xx WARN, 5xx ERROR
- `com.xdyan.commons.http.filter.CorrelationFilter`: MDC fields, request and response bodies in the log (cut at
  `xdyan.commons.http.log.max-body-length`, 500) and Content-Length
- `com.xdyan.commons.http.mapper.DateTimeMapper`

A change to that code is a new version of the library, then the version of the parent here.

The `<parent>` of `pom.xml` is `com.xdyan:quarkus-lambda-parent`, released with the library in the same version. It
decides the versions of Quarkus, the library, Lombok, MapStruct and the plugins, and configures the build (compiler,
surefire, `-Pnative`, resource filtering, the quarkus plugin and the OpenAPI generator). So:
- Dependencies are declared without `<version>`, except one the parent doesn't manage
- Upgrading Quarkus or the library is changing only the version of the parent, never `quarkus.platform.version` here
- The app is contract-first by having `src/main/resources/openapi.yaml`: the parent runs the generator (profile
  `contract-first`) and adds `jackson-databind-nullable`; nothing is declared here. Its options (`containerDefaultToNull`,
  `@RegisterForReflection` on models and enums…) are in the parent. Without that file the app doesn't start in dev and
  tests (`ContractFirstCheck` of the library)

Key wiring:
- MapStruct mappers use `componentModel = "cdi"` and `uses = DateTimeMapper.class` (library); it converts every `LocalDate` to `dd/MM/yyyy`,
  `LocalDateTime` to `dd/MM/yyyy HH:mm` and `LocalTime` to `HH:mm` (strict: `31/02/1985` is a 400, not Feb 28)
- Services return entities or business records; only `api/` knows the generated DTOs
- One service per endpoint, named after its `operationId` (`ListItemsService.listItems`, `GetItemService`, `CreateItemService`); the records it returns live in it
- Services declare every local variable with its type, no `var`, so a review shows what each call returns
- Rules the OpenAPI can't express (e.g. at most 2 decimals, a name already taken) are checked in the service: `BadRequestException` (400) or `ConflictException` (409)
- Partial updates use `PATCH`: fields not sent keep the current value. A form that can empty optional fields sends the whole object to a `PUT`, where not sent or `null` is emptied
- `containerDefaultToNull` (parent) makes a list that is not sent `null`, like any other field, not an empty list
- Money is `BigDecimal` (`type: number` without `format` in the OpenAPI, `DECIMAL(10, 2)` in MySQL)
- Text that is shown in a frontend has a `pattern` in the OpenAPI (e.g. `ItemName`), so it can't carry HTML
- `@RegisterForReflection` on all DTOs and generated enums (`additionalModelTypeAnnotations` and `additionalEnumTypeAnnotations` in the parent), the records built with `SELECT new` and the records and enums Jackson reads — required for native image; the JVM tests don't catch a missing one: it is a 500 only in the lambda, or an enum written with `name()` instead of its `@JsonValue`. `RegisterForReflectionTest` checks every generated model and enum
- Resources read from the classpath at runtime (e.g. JSON files) need `quarkus.native.resources.includes`

## Observability

- **X-Ray** (`quarkus-amazon-lambda-xray`, library): `@XRayTraced` on the `*ResourceImpl`, `*Service` and `*RepositoryImpl` beans records each method call as a subsegment `<layer> <Class>.<method>`, nested API → Service → MySQL, with annotations `layer` and `operation`
  - The layer comes from the package (`api` → `API`, `business.service` → `Service`, `infrastructure.mysql` → `MySQL`): new beans in those packages must be annotated too; `infrastructure.<name>` → `<Name>`, any other package → `App`
  - 4xx (`WebApplicationException` < 500, or an exception implementing `ClientError`) mark the subsegment as `error` with annotation `error_message`; other exceptions are recorded as `fault`
  - The interceptor runs before `@Transactional`, so the subsegment also covers the commit/flush; it is skipped outside Lambda (tests, local run)
- **MDC** (`CorrelationFilter` of the library, cleared after the response is written): `request_id` (from the HTTP API v2 event), `trace_id` (X-Ray), `method`, `path`, `status`, `client_name`/`client_version` (headers `X-Client-Name`/`X-Client-Version`, informational only). The app adds its own fields with an `MdcContributor` bean
- JSON console logs add `app_name`, `app_group`, `app_version`; log level `ERROR` globally, `INFO` for `com.example.app` and `com.xdyan.commons` (set by the library)
- Request and response bodies are logged (truncated at 500 chars): don't send secrets or personal data you can't have in the logs

## Database

- **Config via env vars** (set by `deploy/`): `DB_HOST` (default 127.0.0.1), `DB_USER`, `DB_PASS`, `DB_DATABASE`
- Flyway `migrate-at-start: true`. To adopt an existing database, write its schema as `V1.0.0` and enable `baseline-on-migrate` (commented in `application.yaml`). The DB user needs DDL privileges
- A migration is never changed or deleted once applied: a change is a new `V<n>` file
- Hibernate naming: `PhysicalNamingStrategySnakeCaseImpl` (camelCase → snake_case); `schema-management: none`
- Dates are stored in the zone `xdyan.commons.clock.zone` (`application.yaml`) while Lambda runs in UTC

### Tests use H2 in-memory (no MySQL needed)
- `src/test/resources/application.yaml` overrides to H2 in `MODE=MySQL`, so migrations use MySQL syntax. A column named like an H2 keyword (e.g. `hour`) needs `NON_KEYWORDS=HOUR` in the URL
- A `json` column in the test SQL needs `'{...}' FORMAT JSON`: without it H2 saves the text as a JSON string

## Testing patterns

- `@QuarkusTest` + `@TestHTTPEndpoint(<Resource>.class)` + REST Assured
- One package per resource under `api/` (e.g. `api/item/`) with one `Success` and one `Error` class per endpoint:
  `ItemResource{List,Get,Create}{Success,Error}Test`. An endpoint without possible errors (e.g. `GET /item`) has only `Success`
- Errors are checked with `containsString` on `errors[0].message` (and `code`), never against the whole body: the
  format of the error (`name`, `component`…) is the library's and is tested there
- `FixedClockProducer` fixes "now" at Monday 2025/12/15 12:00 Lima time, replacing the `Clock` of the library
- The data of each test class is inline SQL in its `@BeforeEach @Transactional` (`INSERT INTO ... VALUES`), so the data is
  read in the test itself. Fixed IDs from 101, so they don't collide with the ones generated when a test creates through
  the API. `DatabaseTestUtils.cleanDatabase` deletes in foreign key order in `@AfterEach @Transactional`: add each new entity
- `events/*.json` are sample HTTP API v2 events; `http/*.http` are IntelliJ HTTP Client requests (`local`, `test`), one file per resource

## Documentation

- `docs/api/<resource>/`: a `README.md` with the table of its endpoints and one `.md` per endpoint (`get-item-id.md`, `post-item.md`): request (fields with type, `pattern` of the OpenAPI and rules; required ones marked with `\*` and the legend «\* Obligatorio.») with an example request, responses (a `### <code>` per response with one line of when it happens and its real body, taken from the API, not invented; only the ones of the code of the service, not the 400 of the OpenAPI validation: required fields, pattern, lengths) and a Mermaid sequence diagram. In Spanish
- A new endpoint gets its page and its row in the `README.md` of the resource; a new resource, its row in `docs/README.md`

## Deployment

The app owns its lambda configuration; the infrastructure (Terraform or similar) only creates the platform: functions
`<name>-{test,prod}`, execution role, VPC, X-Ray, log group, the alias `live` that API Gateway invokes, the S3 bucket of
the artifacts and the OIDC role of GitHub Actions.

### Manifest (`deploy/`, configuration only)
- `deploy/base.yaml` + `deploy/environments/<env>.yaml`, deep merged (the environment wins) with yq; `${ENV}` is replaced by the environment
- `spec.memorySize`, `spec.timeout`, `spec.env` (plain) and `spec.secrets` (`ssm:/...`, SecureString resolved at deploy time); a key can't be in both `env` and `secrets`
- SSM parameters per environment, created by hand: `/<env>/quarkus-lambda-http-template/DB_USER` and `DB_PASS`

### Workflows (`.github/workflows/`)
- Each one only calls a reusable workflow of quarkus-lambda-platform (`lambda-pr-validation.yaml`,
  `lambda-deploy-test.yaml`, `lambda-release-production.yaml`) with the **same version as the parent** of `pom.xml`:
  the steps, the scripts (`apply-manifest.sh`, `deploy-code.sh`, `next-version.sh`…) and the schema of `deploy/`
  are there, not in this repository. Upgrade both together (Dependabot opens one PR for each)
- The name of the lambda is `metadata.name` of `deploy/base.yaml`; the repository passes the variables
  `AWS_REGION` and `LAMBDA_ARTIFACTS_BUCKET` and the secret `AWS_ROLE_ARN`

### Versions
- Never in `pom.xml` (`<version>${revision}</version>`, `revision` = `0.0.0-SNAPSHOT`): `maven-git-versioning-extension`
  (`.mvn/extensions.xml`, `.mvn/maven-git-versioning-extension.xml`) sets it from the tags `vX.Y.Z`:
  tagged commit → `X.Y.Z`, after it → `X.Y.Z-<n>-g<sha>-SNAPSHOT`, no tags → `0.0.0-<n>-g<sha>-SNAPSHOT`
- Every checkout that runs Maven needs `fetch-depth: 0`, or the extension can't see the tags
- Tags `v*` are created only by "Deploy to test" (protect them with a tag ruleset); PR validation fails if `<version>`/`<revision>` change

### CI/CD (AWS role by OIDC, secret `AWS_ROLE_ARN`, trusted only on develop and main; variables `LAMBDA_ARTIFACTS_BUCKET` and optional `AWS_REGION`)
Deploys only on demand (`workflow_dispatch`), nothing on push:
1. **PR → develop or main** (`pr-validation.yaml`) — version guard + JVM tests + manifests of `deploy/` (`--dry-run`). No native build, no AWS
2. **Deploy to test** (`deploy-test.yaml`, ref develop, inputs `bump` and `description`) — next version → tests → tag →
   native build → push tag → upload `<name>-X.Y.Z.zip` (the tag is deleted if the upload fails) → pre-release →
   manifest → code → alias `live` of `-test`. If the deploy job fails, "Re-run failed jobs" redeploys the same version
3. **Release to production** (`release-production.yaml`, input `notes`) — latest pre-release → checks `-test:live`
   runs that zip (CodeSha256) → fast-forwards `main` to the tag → manifest + same zip to `-prod` → promotes the release

- Each published version has its code and its configuration: rollback = `aws lambda update-alias --name live --function-version <previous>`
- User-facing steps (versions, deploys, rollback, GitHub settings) are in `README.md`

### TODOs with a removal condition
A `TODO` always says when it can be removed. Before each deploy, list them with `grep -rn "TODO" src` and check each
condition: met → remove the code in a PR; not met → keep it and say which condition is still missing.
