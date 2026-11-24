# quarkus-lambda-commons

Código compartido de Quarkus para lambdas de AWS, y el parent que decide con qué versiones se construyen. Los beans
de los módulos se activan solos, sin declarar nada: los jars traen su índice Jandex.

| Artefacto | Para | Trae |
|---|---|---|
| `quarkus-lambda-parent` | El `<parent>` de cada lambda | Las versiones (de `quarkus-lambda-dependencies`); las dependencias que usa toda lambda: `quarkus-config-yaml`, `quarkus-logging-json`, Lombok, MapStruct y `quarkus-junit`; y el build: el plugin de Quarkus, el perfil `native` y el generador del OpenAPI |
| `quarkus-lambda-dependencies` | El parent de la librería y de `quarkus-lambda-parent`. Una app no lo usa | Solo las versiones: Quarkus, los módulos de abajo, Lombok, MapStruct y los plugins |
| `quarkus-lambda-code-rules` | Lo leen los plugins de las reglas del código. Una app no lo declara | La configuración de Checkstyle y PMD |
| `quarkus-lambda-http-archetype` | Crear una lambda HTTP nueva | El proyecto completo: parent, `openapi.yaml`, el recurso de ejemplo `Item` con sus tests, `deploy/`, los workflows y los docs |
| `quarkus-lambda-commons-core` | Cualquier lambda (HTTP, SQS, programada) | `quarkus-amazon-lambda-xray`, `@XRayTraced` (subsegmentos de X-Ray por capa) y el bean `Clock` |
| `quarkus-lambda-commons-http` | Lambdas de API HTTP. Incluye `core` | El stack HTTP: `quarkus-amazon-lambda-http`, `quarkus-resteasy-jackson` y `quarkus-hibernate-validator`, que la app no declara. Respuestas de error `{name, message, code, errors[]}` para 400, 404 y 409, `CorrelationFilter` (MDC y logs de los bodies) y `DateTimeMapper` (`dd/MM/yyyy`) |

La versión de Quarkus de cada release está en `quarkus-lambda-dependencies/pom.xml` (`quarkus.platform.version`); hoy
es 3.34.4. Todos salen juntos con la misma versión. El parent de una versión usa los módulos de esa misma versión y la versión de
Quarkus con la que se probaron, así que una app nunca mezcla versiones incompatibles.

## Crear una lambda

Con el archetype de la misma versión que se quiere usar (Java 25 y Maven):

```bash
mvn archetype:generate -B \
  -DarchetypeGroupId=com.xdyan \
  -DarchetypeArtifactId=quarkus-lambda-http-archetype \
  -DarchetypeVersion=0.5.0 \
  -DgroupId=com.acme.orders \
  -DartifactId=lambda-http-orders \
  -DrootPath=/orders/v1
cd lambda-http-orders && ./mvnw clean verify
```

| Propiedad | Qué es | Por defecto |
|---|---|---|
| `artifactId` | Nombre en kebab-case: también el de las funciones (`<nombre>-test`, `<nombre>-prod`), la carpeta de S3 y los parámetros de SSM | — |
| `groupId` | El código queda en `<groupId>.app` y la API generada en `<groupId>.api` | — |
| `rootPath` | Ruta base de la API (`quarkus.http.root-path` y `servers` del `openapi.yaml`) | `/api/v1` |
| `package` | Paquete del código, si no es el `groupId` | El `groupId` |

El proyecto sale con el parent y los workflows de esa versión, y pasa sus tests y las reglas del código. Al terminar, el
archetype lista los `CHANGE_ME` que faltan (el host de la base por ambiente y la URL de test); los demás pasos están en
el README del proyecto.

## Uso

```xml
<parent>
  <groupId>com.xdyan</groupId>
  <artifactId>quarkus-lambda-parent</artifactId>
  <version>0.3.0</version>
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
    <!-- Configurado en el parent: basta con declararlo -->
    <plugin>
      <groupId>io.quarkus</groupId>
      <artifactId>quarkus-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

- **Contract-first**: una app con `src/main/resources/openapi.yaml` genera sola sus interfaces JAX-RS
  (`<groupId>.api`) y sus DTOs (`<groupId>.api.model`) en `target/`, sin declarar el generador (perfil
  `contract-first` del parent). Con `quarkus-lambda-commons-http` el contrato es obligatorio: sin ese archivo la app no
  arranca en dev ni en los tests, con un mensaje que lo dice. En la lambda no se revisa
- **Para subir Quarkus o la librería** se cambia solo la versión del parent
- El parent también configura el compilador (Java 25, Lombok y MapStruct), los tests (surefire con el LogManager de
  Quarkus), el filtrado de `src/main/resources` (`${...}` de Maven) y `./mvnw package -Pnative`
- Una dependencia que el parent no maneja se declara con su versión, como en cualquier pom
- Una app no puede quitar una dependencia que hereda: por eso el parent solo trae las que usa toda lambda. El stack
  HTTP llega con `quarkus-lambda-commons-http`; lo demás (base de datos, `rest-assured`) lo declara cada app
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
      openapi:
        required: true            # false solo para una lambda HTTP sin contrato, p. ej. un webhook (default true)
```

`name` y `component` de los errores son `quarkus.application.name`, que Quarkus llena con el `artifactId` de la app.

### Campos nulos en las respuestas

`commons-http` no escribe los campos nulos: la respuesta no trae `"campo": null`, el campo simplemente no está (una
lista vacía sí se escribe, `[]`). Por eso, en el `openapi.yaml` un campo que puede faltar es opcional: no va en
`required` con `nullable: true`. Una app que necesite los `null` (p. ej. un frontend que ya compara con `null`) lo
vuelve a activar:

```yaml
quarkus:
  jackson:
    serialization-inclusion: always
```

## Reglas del código

El parent revisa el código de cada lambda, igual que en esta librería. Una violación hace fallar el build:

- **Checkstyle** antes de compilar (`validate`) y **PMD** después de compilar (`process-classes`): los dos corren en
  cualquier `./mvnw test`
- **El formato** en `./mvnw verify` (y en el PR validation): un espacio de más no detiene los tests mientras se
  desarrolla

| Herramienta | Revisa | Configuración |
|---|---|---|
| Spotless | El formato: google-java-format (2 espacios, 100 columnas, orden de imports) y que no haya imports sin usar | No se configura |
| Checkstyle | Nombres, imports con `*`, llaves en todo `if`/`for`/`while`, `equals` y `hashCode` juntos, `switch` con `default` y sin caer al siguiente `case`, `==` entre strings, javadoc en las interfaces públicas, código comentado | `quarkus-lambda-code-rules`: `quarkus-lambda/checkstyle.xml` |
| PMD | Bugs y malas prácticas: código sin usar, `System.out`, excepciones perdidas o vacías, `catch (Exception e)`, recursos sin cerrar, comparaciones | `quarkus-lambda-code-rules`: `quarkus-lambda/pmd-ruleset.xml` |

- **`./mvnw spotless:apply`** formatea el código: el formato nunca se arregla a mano. Conviene configurar el IDE con
  google-java-format (plugin de IntelliJ) para que formatee igual
- Solo se revisa `src/main/java` y `src/test/java`: el código generado del `openapi.yaml` no
- Un comentario `//` de más de 100 columnas se corta en dos líneas: hay que reescribirlo
- Una excepción a una regla va en el lugar, con el motivo en un comentario:
  `@SuppressWarnings("PMD.AvoidCatchingGenericException")` o `@SuppressWarnings("checkstyle:membername")`
- Un plugin entero se apaga con `-Dspotless.check.skip`, `-Dcheckstyle.skip` o `-Dpmd.skip` (en el pom de la app, como
  propiedad, si es para siempre)

## Workflows de las lambdas

El CI/CD de una lambda también vive aquí, como *reusable workflows*, para que un arreglo llegue a todas las lambdas
con la versión de la librería y no haya que copiarlo en cada repo:

| Workflow | Hace |
|---|---|
| `lambda-pr-validation.yaml` | Que el PR no cambie la versión, los tests JVM, las reglas del código (`verify`) y los manifests de `deploy/` (sin AWS) |
| `lambda-deploy-test.yaml` | Siguiente versión → tests → tag → build nativo → zip a S3 → pre-release → manifest y código en `<nombre>-test` |
| `lambda-release-production.yaml` | La última pre-release que corre en `-test` → mueve `main` → el mismo zip en `<nombre>-prod` → release |

La lambda tiene tres workflows de pocas líneas que los llaman **con la misma versión que su parent** (el ejemplo
completo está al inicio de cada workflow):

```yaml
jobs:
  deploy:
    uses: User-Jean/quarkus-lambda-commons/.github/workflows/lambda-deploy-test.yaml@v0.4.0
    permissions:
      contents: write
      id-token: write
    with:
      bump: ${{ inputs.bump }}
      description: ${{ inputs.description }}
      aws-region: ${{ vars.AWS_REGION || 'us-east-1' }}
      artifacts-bucket: ${{ vars.LAMBDA_ARTIFACTS_BUCKET }}
    secrets:
      AWS_ROLE_ARN: ${{ secrets.AWS_ROLE_ARN }}
```

- El nombre de la lambda es `metadata.name` de `deploy/base.yaml`: las funciones son `<nombre>-test` y `<nombre>-prod`,
  y los zips van a `s3://<bucket>/<nombre>/<nombre>-X.Y.Z.zip`
- Los scripts (`.github/lambda/`: `apply-manifest.sh`, `deploy-code.sh`, `check-version-unchanged.sh` y el schema del
  manifest; `.github/scripts/`: `next-version.sh` y `release-notes.sh`) se descargan en el mismo commit del workflow
  (`job.workflow_sha`): una lambda en `v0.4.0` corre los scripts de `v0.4.0`
- Dependabot de la lambda (`github-actions`) abre el PR cuando sale una versión nueva, igual que el del parent (`maven`):
  conviene subir los dos juntos
- Para probar un cambio antes del release, la lambda apunta a la rama (`@mi-rama`) en un PR

## Para reemplazar algo

- **`Clock`**: si la app produce su propio `Clock`, gana el de la app (`@DefaultBean`)
- **Qué es error y qué es fault en X-Ray**: con `commons-http`, los 4xx de JAX-RS son `error` (del cliente) y lo demás
  `fault`. Una excepción propia que sea del cliente implementa `ClientError`

## Versiones

La versión no está en `pom.xml`: sale de los tags `vX.Y.Z` (`.mvn/`), igual que en las lambdas. Sin tag,
`./mvnw install` deja en el repositorio local de Maven una versión `X.Y.Z-<n>-g<sha>-SNAPSHOT` para probarla en una app.

### Dependencias

Dependabot (`.github/dependabot.yml`) abre un PR cada lunes si hay versiones nuevas de Quarkus o de los plugins, y cada
mes si hay de las actions de GitHub. El PR validation corre los tests en cada uno. Hacer merge no publica nada: lo
publica el siguiente Release. Una versión nueva de Quarkus cambia con qué apps es compatible la librería, así que sale
en su propio release `minor` y su descripción dice la versión de Quarkus.

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
