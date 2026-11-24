# GET /item/{id}

[← Item](README.md)

Obtiene un item por su ID.

## Request

| Parámetro | En | Tipo |
|---|---|---|
| `id` \* | path | integer |

\* Obligatorio.

```http
GET /api/v1/item/2
```

## Respuestas

### 200

El item existe.

```json
{
  "id": 2,
  "name": "Mouse",
  "unit_price": 59.90,
  "created_at": "15/12/2025 12:00"
}
```

### 404

No existe un item con ese ID.

```json
{
  "name": "quarkus-lambda-http-template",
  "message": "Resource not found",
  "code": 404,
  "errors": [
    {
      "message": "Item not found",
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
    participant Service as GetItemService
    participant Repo as ItemRepositoryImpl
    participant DB as MySQL

    Cliente->>API: GET /item/{id}
    API->>Service: getItem(id)
    Service->>Repo: findById(id)
    Repo->>DB: SELECT ... FROM items WHERE id = ?
    alt Existe
        DB-->>Repo: fila
        Service-->>API: item
        API-->>Cliente: 200 Item
    else No existe
        DB-->>Repo: vacío
        Service-->>Cliente: 404 "Item not found"
    end
```
