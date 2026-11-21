# quarkus-lambda-commons

Código compartido de Quarkus para lambdas de AWS. Cada lambda agrega solo el módulo que necesita y los beans se activan
solos, sin declarar nada: los jars traen su índice Jandex.

| Módulo | Para | Trae |
|---|---|---|
| `quarkus-lambda-commons-core` | Cualquier lambda (HTTP, SQS, programada) | `@XRayTraced` (subsegmentos de X-Ray por capa) y el bean `Clock` |
| `quarkus-lambda-commons-http` | Lambdas de API HTTP. Incluye `core` | Respuestas de error `{name, message, code, errors[]}` para 400, 404 y 409, `CorrelationFilter` (MDC y logs de los bodies) y `DateTimeMapper` (`dd/MM/yyyy`) |

Usa Quarkus 3.34.4: la app debe usar la misma versión o una compatible.

## Uso

```xml
<dependency>
  <groupId>com.xdyan</groupId>
  <artifactId>quarkus-lambda-commons-http</artifactId>
  <version>0.1.0</version>
</dependency>
```

Lo que la app sigue escribiendo:

- `@XRayTraced` en sus `*ResourceImpl`, `*Service` y `*RepositoryImpl`. La capa sale del paquete: `api` → `API`,
  `business.service` → `Service`, `infrastructure.mysql` → `MySQL`, `infrastructure.<nombre>` → `<Nombre>`
- `@Inject Clock clock` donde necesite "ahora", y en los tests un productor `@Mock` que lo fije
- `uses = DateTimeMapper.class` en sus mappers de MapStruct
- `ConflictException` (409) e `InvalidFieldsException` (400, un error por campo) en sus servicios; `NotFoundException`
  y `BadRequestException` son las de JAX-RS. No hace falta escribir el error en el log: lo hace la librería

## Logs de un request

Con el MDC del request (`requestId`, `trace_id`, `path`, `client_name`) en cada línea:

```
INFO  Request Body: {"name": "Teclado", "unit_price": 10.123}
WARN  [ERROR 400] BadRequestException: The field 'unit_price' must have at most 2 decimals
INFO  Response 400: Body: {"name":"lambda-http-orders","message":"Validation error occurred",...}
```

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
./mvnw -Prelease -Dgpg.skip -DskipTests clean verify
```
