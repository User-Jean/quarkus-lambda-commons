# AGENTS.md — quarkus-lambda-commons

Library shared by the Quarkus lambdas. Two modules, one version (tags `vX.Y.Z`, as in the lambdas):

```
com.xdyan.commons
├── xray/    (core) @XRayTraced, XRayTracedInterceptor, ClientError, ErrorClassifier (+ DefaultErrorClassifier)
├── clock/   (core) ClockProducer (@DefaultBean), ClockConfig
└── http/    (http) HttpConfig
    ├── error/    ErrorMessage, ErrorLog, ConflictException, InvalidFieldsException, HttpErrorClassifier, provider/*
    ├── filter/   CorrelationFilter
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
- Nothing of a client or a domain: this repository is meant to be public
- Every public class has a test that starts Quarkus (`@QuarkusTest` in the module, which acts as the app). The test
  config is `application.properties`: the modules don't have `quarkus-config-yaml`
- `./mvnw clean install` runs the tests of both modules and installs the jars in the local Maven repository
