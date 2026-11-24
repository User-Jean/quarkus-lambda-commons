# POST /item

[← Item](README.md)

Crea un item. `created_at` es la fecha y hora del momento de la creación.

## Request

| Campo | Tipo | Pattern | Reglas |
|---|---|---|---|
| `name` \* | string | `^[\p{L}0-9 '().-]+$` | 1–100 caracteres: letras, números, espacios y `' ( ) . -`. Único |
| `description` | string | `^[^<>]*$` | 1–500 caracteres, sin `<` ni `>` |
| `unit_price` \* | number | — | 0 o más, máximo 2 decimales |

\* Obligatorio.

```http
POST /api/v1/item
Content-Type: application/json

{"name": "Teclado mecánico", "description": "Switches marrones", "unit_price": 149.90}
```

## Respuestas

### 201

Se creó el item. `created_at` es el momento de la creación y `unit_price` siempre tiene 2 decimales.

```json
{
  "id": 1,
  "name": "Teclado mecánico",
  "unit_price": 149.90,
  "created_at": "15/12/2025 12:00",
  "description": "Switches marrones"
}
```

### 400

El precio tiene más de 2 decimales, por ejemplo `{"name": "Cable", "unit_price": 10.123}`.

```json
{
  "name": "quarkus-lambda-http-template",
  "message": "Validation error occurred",
  "code": 400,
  "errors": [
    {
      "message": "The field 'unit_price' must have at most 2 decimals",
      "component": "quarkus-lambda-http-template"
    }
  ]
}
```

### 409

Ya existe un item con ese nombre.

```json
{
  "name": "quarkus-lambda-http-template",
  "message": "Conflict with the current state",
  "code": 409,
  "errors": [
    {
      "message": "An item named 'Teclado mecánico' already exists",
      "component": "quarkus-lambda-http-template"
    }
  ]
}
```

## Diagrama de secuencia

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant API as ItemResourceImpl
    participant Service as CreateItemService
    participant Repo as ItemRepositoryImpl
    participant DB as MySQL

    Cliente->>API: POST /item {name, description, unit_price}
    alt No cumple el OpenAPI (falta un campo, pattern, longitud, negativo)
        API-->>Cliente: 400 (Hibernate Validator, antes del servicio)
    end
    API->>Service: createItem(item)
    alt unit_price con más de 2 decimales
        Service-->>Cliente: 400 "must have at most 2 decimals"
    end
    Service->>Repo: existsByName(name)
    Repo->>DB: SELECT COUNT(*) FROM items WHERE name = ?
    DB-->>Repo: n
    alt Ya existe
        Service-->>Cliente: 409 "An item named '...' already exists"
    end
    Service->>Service: created_at = now (Clock)
    Service->>Repo: create(item)
    Repo->>DB: INSERT INTO items
    DB-->>Repo: id
    Service-->>API: item
    API-->>Cliente: 201 Item
```
