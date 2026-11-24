# quarkus-lambda-http-template

API HTTP en AWS Lambda con Quarkus: OpenAPI-first, imagen nativa, MySQL con Flyway, X-Ray y CI/CD con GitHub Actions
(OIDC). Generada con el archetype `quarkus-lambda-http-archetype` de
[quarkus-lambda-commons](https://github.com/User-Jean/quarkus-lambda-commons). Los detalles técnicos y las convenciones
están en [AGENTS.md](AGENTS.md).

## Qué trae

| Parte | Qué hace |
|---|---|
| OpenAPI-first | `openapi.yaml` genera los DTOs y las interfaces JAX-RS (`./mvnw generate-sources`); el código solo las implementa |
| Lambda nativa | `quarkus-amazon-lambda-http` + GraalVM, runtime `provided.al2023` |
| MySQL + Flyway | Migraciones al arrancar; los tests usan H2 en modo MySQL, sin Docker |
| Errores | Un solo formato `{name, message, code, errors[]}` para 400, 404 y 409 |
| Observabilidad | Subsegmentos de X-Ray por capa (API → Service → MySQL), logs JSON con `request_id`, `trace_id` y la versión |
| Despliegue | Manifiesto en `deploy/` (memoria, timeout, variables, secretos de SSM), versiones por tags `vX.Y.Z`, mismo zip de test a prod |
| quarkus-lambda-commons | El parent (`quarkus-lambda-parent`) decide Quarkus, las librerías y los plugins; `commons-http` trae los errores, los logs, X-Ray y el `Clock`. Las respuestas no escriben los campos nulos. Para actualizar, solo se cambia la versión del parent |
| Dependabot | Un PR al mes con las versiones nuevas del parent y de las actions |
| Ejemplo | `GET /hello` (health) y el recurso `Item` con sus tests, para copiar el patrón |

## Primeros pasos

El proyecto ya tiene el nombre, el paquete y la ruta base que se dieron al generarlo.

1. Completa los `CHANGE_ME`: el host de la base por ambiente (`deploy/environments/`) y la URL de test (`http/`).
   `grep -rn CHANGE_ME .` los lista.
2. Revisa que todo pase: `./mvnw clean verify` (tests y reglas del código).
3. Reemplaza el recurso de ejemplo `Item` con los tuyos: [docs/api/item/README.md](docs/api/item/README.md) lista sus
   archivos.
4. Súbelo a GitHub y crea la rama `develop`: los PR van a `develop` y "Release to production" mueve `main`.

   ```bash
   git init -b main && git add -A && git commit -m "Initial commit"
   gh repo create <nombre> --private --source . --push
   git switch -c develop && git push -u origin develop
   ```

## Configuración en AWS y GitHub

Una vez por proyecto:

- **Infraestructura** (Terraform u otro): funciones `<nombre>-test` y `<nombre>-prod` (runtime `provided.al2023`,
  handler cualquiera, en la VPC de la base), el alias `live` que invoca API Gateway, el bucket S3 de los zips y un rol
  de IAM para GitHub Actions por OIDC que solo confíe en `develop` y `main` de este repo.
- **Parámetros SSM** (SecureString): `/test/<nombre>/DB_USER`, `/test/<nombre>/DB_PASS` y los mismos para `prod`.
- **Secret** `AWS_ROLE_ARN`: el ARN de ese rol.
- **Variables** (Settings → Secrets and variables → Actions → Variables): `LAMBDA_ARTIFACTS_BUCKET` (el bucket) y, si no
  es `us-east-1`, `AWS_REGION`.
- **Ruleset de tags**: Settings → Rules → Rulesets → New tag ruleset, tags `v*`, con *Restrict creations*, *Restrict
  updates* y *Restrict deletions*, y **GitHub Actions** en la bypass list: solo los workflows crean versiones.
- **Protección de `main`**: si la agregas, pon **GitHub Actions** en la bypass list, porque "Release to production" hace
  push a `main`.

## Versiones

La versión **no está en `pom.xml`**: sale de los tags `vX.Y.Z`, que solo crea "Deploy to test".

| Commit | Versión del build |
|---|---|
| Commit con el tag `v1.4.0` | `1.4.0` |
| 3 commits después de `v1.4.0` | `1.4.0-3-g<sha>-SNAPSHOT` |
| Sin tags | `0.0.0-<commits>-g<sha>-SNAPSHOT` |

Un PR que cambie `<version>` o `<revision>` en `pom.xml` falla en la validación.

## Desplegar

Desde la pestaña **Actions**, a mano; un push no despliega nada.

| Workflow | Ref | Qué hace |
|---|---|---|
| Deploy to test | `develop` | Calcula la versión (`bump`), corre los tests, compila el nativo, crea el tag, sube el zip, crea el pre-release con el changelog y lo despliega en `-test` |
| Release to production | `develop` o `main` | Toma el último pre-release, verifica que sea el que corre en test, mueve `main` y despliega el mismo zip en `-prod` |

El primer despliegue es "Deploy to test" con bump `minor` (`0.1.0`). Si la función todavía no existe, el job de deploy
falla: créala y usa **Re-run failed jobs**, que despliega la misma versión.

Rollback de prod: cada versión de la Lambda tiene su código y su configuración.

```bash
aws lambda list-versions-by-function --function-name <nombre>-prod \
  --query 'Versions[].[Version,Description]' --output table
aws lambda update-alias --function-name <nombre>-prod --name live --function-version <N>
```
