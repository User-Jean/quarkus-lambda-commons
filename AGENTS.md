# AGENTS.md — quarkus-lambda-commons

Library shared by the Quarkus lambdas. Two modules, one version (tags `vX.Y.Z`, as in the lambdas):

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
- Every module has the `jandex-maven-plugin` (parent pom): without `META-INF/jandex.idx` the apps don't discover its
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
- Published to Maven Central by the workflow `release.yaml` (profile `release` of the parent pom: sources, javadoc,
  GPG signature, `central-publishing-maven-plugin`). A published version is permanent: a fix is a new version
- The metadata Maven Central requires (url, licenses, developers, scm) is in the parent pom and inherited as is; a new
  module needs its own `name` and `description`
