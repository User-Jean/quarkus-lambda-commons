# AGENTS.md — quarkus-lambda-platform

Library shared by the Quarkus lambdas, and the parent pom of the lambdas. Four artifacts, one version (tags `vX.Y.Z`,
as in the lambdas):

```
pom.xml                         quarkus-lambda-commons-parent: build of the library (modules, Jandex, Lombok, tests)
├── quarkus-lambda-dependencies/  versions, metadata and publishing: parent of pom.xml and of quarkus-lambda-parent
├── quarkus-lambda-code-rules/    the Checkstyle and PMD configuration (child of quarkus-lambda-dependencies)
├── quarkus-lambda-parent/        parent of the apps: + the dependencies every lambda uses and the build
├── quarkus-lambda-http-archetype/ the project of a new HTTP lambda (mvn archetype:generate)
├── commons-core/             quarkus-lambda-commons-core
└── commons-http/             quarkus-lambda-commons-http

.github/workflows/lambda-*.yaml   reusable workflows of the lambdas (workflow_call): PR validation, deploy to test, release
.github/lambda/                   their scripts and the schema of deploy/; .github/scripts/ is shared with the library
```


```
com.xdyan.commons
├── xray/    (core) @XRayTraced, XRayTracedInterceptor, ClientError, ErrorClassifier (+ DefaultErrorClassifier)
├── clock/   (core) ClockProducer (@DefaultBean), ClockConfig
└── http/    (http) HttpConfig
    ├── error/    ErrorMessage, ErrorLog, ConflictException, InvalidFieldsException, HttpErrorClassifier, provider/*
    ├── contract/ ContractFirstCheck
    ├── filter/   CorrelationFilter, MdcContributor
    └── mapper/   DateTimeMapper
```

## Rules

- `core` must work in any lambda: no JAX-RS, RESTEasy or API Gateway classes. What depends on HTTP goes in `http`
- The HTTP lambdas are contract-first: the profile `contract-first` of `quarkus-lambda-parent` (active when the app has
  `src/main/resources/openapi.yaml`) runs the generator and adds `jackson-databind-nullable`; `ContractFirstCheck`
  fails the start in dev and tests without that file (`xdyan.commons.http.openapi.required=false` turns it off). Not
  checked in prod: the native image doesn't include the file
- Each module is the starter of its kind of lambda: `http` brings the HTTP stack (`quarkus-amazon-lambda-http`,
  `quarkus-resteasy-jackson`, `quarkus-hibernate-validator`) and the apps don't declare it. Changing or removing one of
  those dependencies breaks the apps: say it in the release
- Every module has the `jandex-maven-plugin` (root pom): without `META-INF/jandex.idx` the apps don't discover its
  beans, providers or interceptors
- A bean an app may want to replace is `@DefaultBean`
- Configuration is a `@ConfigMapping` with prefix `xdyan.commons.<module or component>`, every property with
  `@WithDefault`. A JAX-RS `@Provider` reads it with `Instance<...>` when used: RESTEasy creates the providers before
  the mappings are registered (`SRCFG00027: Could not find a mapping`)
- Every error provider calls `ErrorLog.log(status, exception)` before returning: 4xx WARN without stack trace, 5xx
  ERROR with it. `UnexpectedErrorProvider` (`ExceptionMapper<Exception>`) catches what has no provider; a 500 never
  returns the message of the exception
- `META-INF/microprofile-config.properties` of each module sets `com.xdyan.commons` to INFO: the apps log only ERROR
  outside their package
- `commons-http` sets `quarkus.jackson.serialization-inclusion=non_null` there: a null field is not written in the
  response (an empty list is). A field that can be missing is optional in the `openapi.yaml`, not `required` +
  `nullable`. An app overrides it in its `application.yaml` (`always`). `NullFieldsTest` checks it
- MDC fields are snake_case (`request_id`, `trace_id`, `client_name`…) and listed in `CorrelationFilter.FIELDS`: an
  `MdcContributor` of the app can't replace them. Renaming one breaks the log queries of the apps: say it in the release
- Error messages never show internal details: a 5xx never returns the message of the exception, and the 404 of RESTEasy
  itself (`RESTEASY...`, no endpoint or a path parameter that can't be converted) returns "Resource not found"
- The rules of the code run the same in the library (root pom) and in the apps (`quarkus-lambda-parent`): Checkstyle
  in `validate` (before compiling), PMD in `process-classes` (it needs the classes, so `./mvnw test` runs both) and
  Spotless with google-java-format in `verify`, configured in `quarkus-lambda-dependencies` with their config in
  `quarkus-lambda-code-rules`. Format with `./mvnw spotless:apply`, never by hand. A rule added there fails the build
  of the apps that break it: say it in the release. `quarkus-lambda-code-rules` has `quarkus-lambda-dependencies` as
  parent, not the root pom, which runs the plugins that depend on it. A suppression goes where it applies, with the
  reason in a comment
- Comments: a public interface has a javadoc that says what it is for (Checkstyle). A `//` comment explains why, never
  what the code already says: if the code needs one to be understood, rename or split it first. Commented-out code is
  deleted, it is in git (Checkstyle). The same holds for the lambdas: `quarkus-lambda-code-rules` checks the first and
  the last
- `quarkus-lambda-http-archetype`: `archetype-resources/` is a real project (`com.example`, `quarkus-lambda-http-template`,
  `/api/v1`), copied without Velocity; `archetype-post-generate.groovy` replaces those names, the token
  `QUARKUS_LAMBDA_COMMONS_VERSION` (parent and workflows) and renames `__gitignore__`. Its test generates a project and
  runs `verify`, so the generated code must follow the rules of the code. A change to the parent, `commons-http` or the
  workflows that a lambda would need is made in `archetype-resources/` too
- Nothing of a client or a domain: this repository is meant to be public
- The `lambda-*.yaml` workflows are called by the lambdas with the tag of their parent: they check out their scripts
  with `job.workflow_repository` and `job.workflow_sha` into `.lambda-platform/` (after the checkout of the lambda, whose
  clean would delete them) and run them from the root of the lambda. A new required input or secret, or a change to the
  `deploy/` schema, breaks the lambdas: say it in the release. The example caller at the top of each one is kept up to date
- Every public class has a test that starts Quarkus (`@QuarkusTest` in the module, which acts as the app). The test
  config is `application.properties`: the modules don't have `quarkus-config-yaml`
- `./mvnw clean install` runs the tests of both modules and installs the jars in the local Maven repository
- `quarkus-lambda-dependencies` is the only place with versions: Quarkus (`quarkus.platform.version`, the BOM and the
  plugin), the library modules (`quarkus-lambda-commons.version`, written by the versioning extension, never by hand:
  `${project.version}` would be the version of the app), Lombok, MapStruct and the plugins
- The library inherits from `quarkus-lambda-dependencies`, never from `quarkus-lambda-parent`: the `<dependencies>` of
  the app parent would reach every app through the modules. What the library alone needs goes in the root `pom.xml`
- `<dependencies>` of `quarkus-lambda-parent` only has what every lambda uses (YAML config, JSON logs, Lombok,
  MapStruct, quarkus-junit): an app can't remove an inherited dependency. HTTP, database or SQS dependencies are
  declared by each app
- A change in `quarkus-lambda-parent` reaches every app on its next upgrade: a breaking one (a removed property, a
  changed plugin configuration, a new Quarkus minor) is said in the release
- Published to Maven Central by the workflow `release.yaml` (profile `publish-central` of `quarkus-lambda-dependencies`:
  sources, javadoc, GPG signature, `central-publishing-maven-plugin`). A published version is permanent: a fix is a
  new version
- The metadata Maven Central requires (url, licenses, developers, scm) is in `quarkus-lambda-dependencies` and inherited as
  is; a new module needs its own `name` and `description`
- Dependabot (`.github/dependabot.yml`) proposes the new versions of Quarkus (one group), the build plugins and the
  actions. A Quarkus upgrade (`quarkus.platform.version` in `quarkus-lambda-dependencies`) reaches every app with the
  parent: it is its own minor release, and its description says the Quarkus version
- No pom writes a version: `.github/scripts/check-version-unchanged.sh` (PR validation) fails otherwise
