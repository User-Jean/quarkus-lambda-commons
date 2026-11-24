# Documentación — quarkus-lambda-http-template

Ruta base: `/api/v1`.

El contrato completo (campos, patterns, ejemplos) está en [`openapi.yaml`](../src/main/resources/openapi.yaml).
Estas páginas explican qué hace cada endpoint por dentro: una por endpoint, en `docs/api/<recurso>/`.

## API

| Recurso | Endpoints |
|---|---|
| [Health](api/hello.md) | `GET /hello` |
| [Item](api/item/README.md) | `GET /item`, `GET /item/{id}`, `POST /item` (ejemplo del archetype: reemplázalo) |
