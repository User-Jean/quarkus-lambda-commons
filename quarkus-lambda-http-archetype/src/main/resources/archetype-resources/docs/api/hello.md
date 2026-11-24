# GET /hello

[← Documentación](../README.md)

Health check: confirma que la lambda está arriba y que llega a la base de datos, sin tocar datos. Úsalo después de
cada deploy o para monitoreo.

## Request

Sin parámetros ni body.

```http
GET /api/v1/hello
```

## Respuestas

### 200

La lambda y la base de datos responden.

```json
{
  "message": "Hello",
  "database": "UP"
}
```

### 503

La lambda responde pero la base no: sin conexión o la conexión no es válida en 2 segundos.

```json
{
  "message": "Hello",
  "database": "DOWN"
}
```

## Diagrama de secuencia

```mermaid
sequenceDiagram
    autonumber
    actor Cliente
    participant API as HealthResourceImpl
    participant Service as HealthService
    participant Repo as HealthRepositoryImpl
    participant DB as MySQL

    Cliente->>API: GET /hello
    API->>Service: isDatabaseUp()
    Service->>Repo: ping()
    Repo->>DB: connection.isValid(2 s)
    alt La base responde
        DB-->>Repo: válida
        Repo-->>Service: ok
        Service-->>API: true
        API-->>Cliente: 200 {"message": "Hello", "database": "UP"}
    else Sin conexión o no válida en 2 s
        DB-->>Repo: error / timeout
        Repo-->>Service: IllegalStateException
        Service->>Service: log "Database health check failed"
        Service-->>API: false
        API-->>Cliente: 503 {"message": "Hello", "database": "DOWN"}
    end
```
