# AGENTS.md — quarkus-lambda-commons

Library shared by the Quarkus lambdas, and the parent pom of the lambdas. Three artifacts, one version (tags `vX.Y.Z`,
as in the lambdas):

```
pom.xml                       quarkus-lambda-commons-parent: build of the library (modules, Jandex, test deps)
├── quarkus-lambda-parent/    quarkus-lambda-parent: parent of the apps AND of pom.xml. Versions, metadata, build
├── commons-core/             quarkus-lambda-commons-core
└── commons-http/             quarkus-lambda-commons-http
```


```
com.xdyan.commons
├── xray/    (core) @XRayTraced, XRayTracedInterceptor, ClientError, ErrorClassifier (+ DefaultErrorClassifier)
├── clock/   (core) ClockProducer (@DefaultBean), ClockConfig
└── http/    (http) HttpConfig
    ├── error/    ErrorMessage, ErrorLog, ConflictException, InvalidFieldsException, HttpErrorClassifier, provider/*
    ├── filter/   CorrelationFilter, MdcContributor
    └── mapper/   DateTimeMapper
```

## Rules

- `core` must work in any lambda: no JAX-RS, RESTEasy or API Gateway classes. What depends on HTTP goes in `http`
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
- MDC fields are snake_case (`request_id`, `trace_id`, `client_name`…) and listed in `CorrelationFilter.FIELDS`: an
  `MdcContributor` of the app can't replace them. Renaming one breaks the log queries of the apps: say it in the release
- Error messages never show internal details: a 5xx never returns the message of the exception, and the 404 of RESTEasy
  itself (`RESTEASY...`, no endpoint or a path parameter that can't be converted) returns "Resource not found"
- Nothing of a client or a domain: this repository is meant to be public
- Every public class has a test that starts Quarkus (`@QuarkusTest` in the module, which acts as the app). The test
  config is `application.properties`: the modules don't have `quarkus-config-yaml`
- `./mvnw clean install` runs the tests of both modules and installs the jars in the local Maven repository
- `quarkus-lambda-parent` is the only place with versions: Quarkus (`quarkus.platform.version`, the BOM and the
  plugin), the library modules (`quarkus-lambda-commons.version`, written by the versioning extension, never by hand:
  `${project.version}` would be the version of the app), Lombok, MapStruct and the plugins
- `quarkus-lambda-parent` must not run anything in every module that only an app needs: the quarkus and openapi plugins
  are in `pluginManagement` (the app declares them); dependencies are only managed. What the library alone needs
  goes in the root `pom.xml`
- A change in `quarkus-lambda-parent` reaches every app on its next upgrade: a breaking one (a removed property, a
  changed plugin configuration, a new Quarkus minor) is said in the release
- Published to Maven Central by the workflow `release.yaml` (profile `publish-central` of `quarkus-lambda-parent`:
  sources, javadoc, GPG signature, `central-publishing-maven-plugin`). A published version is permanent: a fix is a
  new version
- The metadata Maven Central requires (url, licenses, developers, scm) is in `quarkus-lambda-parent` and inherited as
  is; a new module needs its own `name` and `description`
- No pom writes a version: `.github/scripts/check-version-unchanged.sh` (PR validation) fails otherwise
