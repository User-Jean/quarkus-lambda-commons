# quarkus-lambda-commons

Código compartido de Quarkus para lambdas de AWS, y el parent que decide con qué versiones se construyen. Los beans
de los módulos se activan solos, sin declarar nada: los jars traen su índice Jandex.

| Artefacto | Para | Trae |
|---|---|---|
| `quarkus-lambda-parent` | El `<parent>` de cada lambda | Las versiones (de `quarkus-lambda-dependencies`); las dependencias que usa toda lambda: `quarkus-config-yaml`, `quarkus-logging-json`, Lombok, MapStruct y `quarkus-junit`; y el build: el plugin de Quarkus, el perfil `native` y el generador del OpenAPI |
| `quarkus-lambda-dependencies` | El parent de la librería y de `quarkus-lambda-parent`. Una app no lo usa | Solo las versiones: Quarkus, los módulos de abajo, Lombok, MapStruct y los plugins |
| `quarkus-lambda-commons-core` | Cualquier lambda (HTTP, SQS, programada) | `@XRayTraced` (subsegmentos de X-Ray por capa) y el bean `Clock` |
| `quarkus-lambda-commons-http` | Lambdas de API HTTP. Incluye `core` | Respuestas de error `{name, message, code, errors[]}` para 400, 404 y 409, `CorrelationFilter` (MDC y logs de los bodies) y `DateTimeMapper` (`dd/MM/yyyy`) |

Todos salen juntos con la misma versión. El parent de una versión usa los módulos de esa misma versión y la versión de
Quarkus con la que se probaron, así que una app nunca mezcla versiones incompatibles.

## Uso

```xml
<parent>
  <groupId>com.xdyan</groupId>
  <artifactId>quarkus-lambda-parent</artifactId>
  <version>0.2.0</version>
  <relativePath/>
</parent>

<dependencies>
  <!-- Sin <version>: la decide el parent. Una lambda SQS o programada usa quarkus-lambda-commons-core -->
  <dependency>
    <groupId>com.xdyan</groupId>
    <artifactId>quarkus-lambda-commons-http</artifactId>
  </dependency>
  <dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-hibernate-orm</artifactId>
  </dependency>
  <!-- quarkus-config-yaml, quarkus-logging-json, Lombok, MapStruct y quarkus-junit ya vienen del parent -->
</dependencies>

<build>
  <plugins>
    <!-- Configurados en el parent: basta con declararlos -->
    <plugin>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-maven-plugin</artifactId>
    </plugin>
    <!-- Solo si la app tiene src/main/resources/openapi.yaml: genera <groupId>.api y <groupId>.api.model -->
    <plugin>
      <groupId>org.openapitools</groupId>
      <artifactId>openapi-generator-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

- **Para subir Quarkus o la librería** se cambia solo la versión del parent
- El parent también configura el compilador (Java 25, Lombok y MapStruct), los tests (surefire con el LogManager de
  Quarkus), el filtrado de `src/main/resources` (`${...}` de Maven) y `./mvnw package -Pnative`
- Una dependencia que el parent no maneja se declara con su versión, como en cualquier pom
- Una app no puede quitar una dependencia que hereda: por eso el parent solo trae las que usa toda lambda. Lo que es
  solo de HTTP (`rest-assured`, `quarkus-resteasy-jackson`) o de una base de datos lo declara cada app
- La app hereda del parent la licencia, la url y el scm de esta librería; no afecta a una lambda, que no se publica

Lo que la app sigue escribiendo:

- `@XRayTraced` en sus `*ResourceImpl`, `*Service` y `*RepositoryImpl`. La capa sale del paquete: `api` → `API`,
  `business.service` → `Service`, `infrastructure.mysql` → `MySQL`, `infrastructure.<nombre>` → `<Nombre>`
- `@Inject Clock clock` donde necesite "ahora", y en los tests un productor `@Mock` que lo fije
- `uses = DateTimeMapper.class` en sus mappers de MapStruct
- `ConflictException` (409) e `InvalidFieldsException` (400, un error por campo) en sus servicios; `NotFoundException`
  y `BadRequestException` son las de JAX-RS. No hace falta escribir el error en el log: lo hace la librería

## Logs de un request

Con el MDC del request en cada línea:

```
INFO  Request Body: {"name": "Teclado", "unit_price": 10.123}
WARN  [ERROR 400] BadRequestException: The field 'unit_price' must have at most 2 decimals
INFO  Response 400: Body: {"name":"lambda-http-orders","message":"Validation error occurred",...}
```

Campos del MDC (`mdc` en el JSON del log):

| Campo | Qué es |
|---|---|
| `request_id` | El id del request en API Gateway. API Gateway lo devuelve al cliente en el header `apigw-requestid`: con él se llega de un error del frontend a sus logs |
| `trace_id` | La traza de X-Ray de la invocación, con los subsegmentos de `@XRayTraced`. Vacío fuera de Lambda |
| `method`, `path`, `status` | El request y el status de la respuesta |
| `client_name`, `client_version` | Los headers `X-Client-Name` y `X-Client-Version`, si el cliente los manda |

El header `X-Amzn-Trace-Id` no está: en un HTTP API de API Gateway no es la traza de X-Ray de la lambda.

### Campos propios de la app

Un bean que implemente `MdcContributor` agrega campos al MDC de cada request, sin declarar nada más:

```java
@ApplicationScoped
public class TenantMdc implements MdcContributor {

  @Override
  public Map<String, String> fields(ContainerRequestContext request) {
    return Map.of("tenant_id", request.getHeaderString("X-Tenant-Id"));
  }
}
```

- Los campos salen en todas las líneas del request, desde `Request Body`, y la librería los limpia al terminar
- Nombres en snake_case, como los de la librería. Un campo de la librería no se puede reemplazar (queda un `WARN`) y un
  valor `null` no se agrega
- Si el bean lanza una excepción, queda en el log como `ERROR` y el request sigue sin esos campos

- Un **4xx** es un `WARN` con la excepción y su mensaje, sin stack trace
- Un **5xx** o una excepción inesperada es un `ERROR` con el stack trace. La respuesta es un 500 con el mismo formato que
  los demás errores, pero sin el mensaje de la excepción, que puede tener detalles internos
- Los logs de la librería salen aunque la app tenga `quarkus.log.level: ERROR`: la librería trae `INFO` para
  `com.xdyan.commons`, y la app lo puede cambiar en su `application.yaml`

## Configuración

Todas las propiedades empiezan con `xdyan.commons`, así se distingue lo que configura la librería de lo de la app.

```yaml
xdyan:
  commons:
    clock:
      zone: America/Lima          # zona de "ahora" y de las fechas guardadas (default America/Lima)
    http:
      log:
        max-body-length: 500      # caracteres de los bodies en el log (default 500)
```

`name` y `component` de los errores son `quarkus.application.name`, que Quarkus llena con el `artifactId` de la app.

## Para reemplazar algo

- **`Clock`**: si la app produce su propio `Clock`, gana el de la app (`@DefaultBean`)
- **Qué es error y qué es fault en X-Ray**: con `commons-http`, los 4xx de JAX-RS son `error` (del cliente) y lo demás
  `fault`. Una excepción propia que sea del cliente implementa `ClientError`

## Versiones

La versión no está en `pom.xml`: sale de los tags `vX.Y.Z` (`.mvn/`), igual que en las lambdas. Sin tag,
`./mvnw install` deja en el repositorio local de Maven una versión `X.Y.Z-<n>-g<sha>-SNAPSHOT` para probarla en una app.

## Publicar

Se publica en Maven Central con el workflow **Release** (Actions → Release → Run workflow, desde `main`):

1. `bump`: `patch` (arreglos), `minor` (algo nuevo compatible) o `major` (cambio que rompe a las apps)
2. `description`: resumen opcional, va al inicio del release de GitHub

El workflow calcula la versión, corre los tests, firma los jars, los sube al Central Portal y espera a que estén
publicados; recién ahí sube el tag y crea el release de GitHub con la lista de PRs. Si falla antes de publicar, no queda
ni tag ni versión: se corrige y se vuelve a correr.

**Una versión publicada en Maven Central no se puede cambiar ni borrar.** Si sale con un error, se publica otra.

### Configuración (una sola vez)

- **Namespace** `com.xdyan` verificado en https://central.sonatype.com (registro TXT en el DNS de `xdyan.com`)
- **User token** del Central Portal (avatar → View Account → Generate User Token)
- **Llave GPG** para firmar, con la parte pública en un keyserver:
  ```bash
  gpg --quick-gen-key "xdyan" ed25519 sign 0
  gpg --keyserver keyserver.ubuntu.com --send-keys <ID de la llave>
  gpg --armor --export-secret-keys <ID de la llave>   # para el secret GPG_PRIVATE_KEY
  ```
- **Secrets** del repositorio (Settings → Secrets and variables → Actions):

  | Secret | Valor |
  |---|---|
  | `CENTRAL_USERNAME` | Username del user token |
  | `CENTRAL_PASSWORD` | Password del user token |
  | `GPG_PRIVATE_KEY` | La llave privada en ASCII (`-----BEGIN PGP PRIVATE KEY BLOCK-----` …) |
  | `GPG_PASSPHRASE` | La frase de la llave |

Para revisar localmente lo que se publicaría (jars de sources y javadoc incluidos), sin firmar ni subir:

```bash
./mvnw -Ppublish-central -Dgpg.skip -DskipTests clean verify
```
