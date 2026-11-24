# Item (ejemplo)

[← Documentación](../../README.md)

Recurso de ejemplo del archetype, para copiar el patrón. Bórralo cuando agregues los recursos de tu servicio.

| Endpoint | Qué hace | Respuestas |
|---|---|---|
| [`GET /item`](get-item.md) | Lista los items ordenados por nombre | 200 |
| [`GET /item/{id}`](get-item-id.md) | Obtiene un item | 200, 404 |
| [`POST /item`](post-item.md) | Crea un item | 201, 400, 409 |

## Para borrarlo

`ItemResourceImpl`, `ItemResourceMapper`, `ItemEntity`, `ItemRepository(Impl)`, `ListItemsService`, `GetItemService`,
`CreateItemService`, `api/item/*Test`, `http/item.http`, esta carpeta `docs/api/item/`,
el tag y los paths `Item` del OpenAPI y la migración `V1.0.0__create_items.sql` (si nunca se aplicó en una base real; si
ya se aplicó, la tabla se borra con una migración nueva). Quita `ItemEntity` de `DatabaseTestUtils`.
