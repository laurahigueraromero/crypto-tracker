# PRD — Crypto Tracker

**Versión:** 1.1 (MVP, en desarrollo)
**Fecha:** 2026-08-16 · última revisión: 2026-08-16
**Stack:** Backend Spring Boot (Java) · Frontend React.js · API externa CoinGecko

> Este documento describe el diseño objetivo del producto. El estado real de
> implementación (qué está construido y qué queda pendiente) se documenta en la
> §8 y se mantiene al día en [`BACKLOG.md`](BACKLOG.md) y las
> [issues de GitHub](../../issues) — consúltalas como fuente de verdad sobre el
> progreso, no este PRD.

---

## 1. Resumen del producto

Aplicación web para el seguimiento de criptomonedas. Permite a un usuario registrarse e iniciar sesión de forma segura (JWT + refresh token), consultar una tabla de estadísticas de mercado en tiempo casi real (precio, variación 24h, capitalización, sparkline), marcar criptomonedas en una watchlist personal, y escribir notas privadas (predicciones, motivos de compra/venta, observaciones) asociadas a una o varias criptomonedas. El backend actúa como proxy con caché frente a CoinGecko, protegiendo la clave de API y controlando el rate limit.

**Objetivo del MVP:** que un usuario pueda registrarse, ver el mercado, seguir criptos de interés y dejar constancia escrita de su razonamiento de inversión, de forma privada y persistente.

---

## 2. Alcance

### 2.1 Dentro del MVP

- Registro y login con JWT (access token 15–30 min) + refresh token con rotación.
- Bloqueo temporal de cuenta tras intentos de login fallidos repetidos.
- Perfil de usuario editable (nombre, avatar, moneda base USD/EUR, zona horaria).
- Tabla de estadísticas: Top N criptos por capitalización, paginada, vía proxy con caché en backend, refresco cada 60s en frontend.
- Selector de moneda base (USD/EUR) para los precios mostrados.
- Búsqueda de cualquier criptomoneda soportada por CoinGecko (no limitado al Top N).
- Watchlist personal independiente de las notas.
- Notas privadas: CRUD completo, 4 tipos fijos, tags libres, asociables a varias criptomonedas.
- Filtro/búsqueda de notas propias por tipo, tag o cripto.
- Validación de formularios en frontend y backend, con sanitización anti-XSS del contenido de notas.
- Rate limiting propio por usuario/IP en la API de Spring Boot.
- Diseño responsive completo (desktop, tablet, móvil).
- Interfaz en español.

### 2.2 Fuera del MVP (roadmap futuro)

- **Alertas de precio**: notificaciones cuando una cripto alcanza un precio objetivo definido por el usuario.
- **Rol de administrador**: panel para gestión de usuarios y contenido. El modelo de datos reserva el campo `role` para soportarlo sin migración disruptiva, pero no hay funcionalidad admin ni endpoints protegidos por rol en el MVP.
- Servir datos de caché "stale" ante fallos de CoinGecko (en el MVP, un fallo de la API externa se traduce en un error explícito al usuario, sin fallback a datos antiguos).
- Gráfico de precio histórico detallado (velas) en la ficha de una cripto.
- Exportación de notas (CSV/PDF).
- Portfolio/cartera simulada con cálculo de ganancias/pérdidas.
- Compartir notas públicamente o con otros usuarios.
- Verificación de email tras el registro.

---

## 3. Modelo de datos

### 3.1 Entidades

#### `User`
| Campo | Tipo | Notas |
|---|---|---|
| id | UUID (PK) | |
| email | string, unique | login |
| passwordHash | string | BCrypt |
| displayName | string | |
| avatarUrl | string, nullable | |
| baseCurrency | enum(`USD`, `EUR`) | default `USD` |
| timezone | string, nullable | IANA tz, ej. `Europe/Madrid` |
| role | enum(`USER`, `ADMIN`) | default `USER`; `ADMIN` reservado, sin uso funcional en MVP |
| failedLoginAttempts | int | default 0, usado para bloqueo temporal |
| lockedUntil | timestamp, nullable | |
| createdAt | timestamp | |
| updatedAt | timestamp | |

#### `RefreshToken`
| Campo | Tipo | Notas |
|---|---|---|
| id | UUID (PK) | |
| userId | FK → User | |
| tokenHash | string | el token en claro nunca se persiste |
| expiresAt | timestamp | |
| revoked | boolean | default false |
| createdAt | timestamp | |

#### `WatchlistItem`
| Campo | Tipo | Notas |
|---|---|---|
| id | UUID (PK) | |
| userId | FK → User | |
| coinId | string | identificador de CoinGecko, ej. `bitcoin` |
| createdAt | timestamp | |

Constraint: único por (`userId`, `coinId`).

#### `Note`
| Campo | Tipo | Notas |
|---|---|---|
| id | UUID (PK) | |
| userId | FK → User | propietario |
| title | string, max 100 | |
| content | text, max 2000 | sanitizado anti-XSS |
| type | enum(`PREDICCION`, `MOTIVO_COMPRA`, `MOTIVO_VENTA`, `OBSERVACION`) | |
| createdAt | timestamp | |
| updatedAt | timestamp | |

#### `NoteCoin` (relación N:M Nota↔Cripto)
| Campo | Tipo | Notas |
|---|---|---|
| noteId | FK → Note | |
| coinId | string | identificador de CoinGecko |

PK compuesta (`noteId`, `coinId`).

#### `NoteTag`
| Campo | Tipo | Notas |
|---|---|---|
| noteId | FK → Note | |
| tag | string, max 30 | |

PK compuesta (`noteId`, `tag`).

> No existe entidad `Crypto` propia: los datos de mercado (nombre, símbolo, logo, precio, etc.) siempre se obtienen en vivo (vía caché) de CoinGecko usando `coinId` como identificador compartido. Todo borrado es físico (sin soft-delete).

### 3.2 Diagrama de relaciones

```
User 1───N RefreshToken
User 1───N WatchlistItem
User 1───N Note 1───N NoteTag
                Note N───N (coinId externo)  [tabla NoteCoin]
WatchlistItem N───1 (coinId externo)
```

---

## 4. Endpoints de la API

Prefijo común: `/api`. Todas las respuestas de error siguen el formato:
```json
{ "timestamp": "2026-08-16T10:00:00Z", "status": 400, "error": "VALIDATION_ERROR", "message": "...", "details": [ ] }
```

### 4.1 Autenticación (`/api/auth`)

#### `POST /api/auth/register`
Request:
```json
{ "email": "user@example.com", "password": "Str0ngPass!", "displayName": "Laura" }
```
Response `201`:
```json
{ "id": "uuid", "email": "user@example.com", "displayName": "Laura" }
```
Errores: `409` email ya existe · `400` password no cumple requisitos mínimos (8+ caracteres, 1 mayúscula, 1 número).

#### `POST /api/auth/login`
Request:
```json
{ "email": "user@example.com", "password": "Str0ngPass!" }
```
Response `200`:
```json
{ "accessToken": "eyJ...", "refreshToken": "eyJ...", "expiresIn": 1800 }
```
Errores: `401` credenciales inválidas · `423` cuenta bloqueada temporalmente por intentos fallidos.

#### `POST /api/auth/refresh`
Request:
```json
{ "refreshToken": "eyJ..." }
```
Response `200`: igual que login (rota el refresh token: el anterior queda revocado).
Errores: `401` refresh token inválido, expirado o revocado.

#### `POST /api/auth/logout`
Request: `{ "refreshToken": "eyJ..." }` → Response `204`. Revoca el refresh token.

### 4.2 Usuario (`/api/users`)

#### `GET /api/users/me` 🔒
Response `200`:
```json
{ "id": "uuid", "email": "user@example.com", "displayName": "Laura", "avatarUrl": null, "baseCurrency": "USD", "timezone": "Europe/Madrid" }
```

#### `PUT /api/users/me` 🔒
Request: `{ "displayName": "Laura H.", "avatarUrl": "...", "baseCurrency": "EUR", "timezone": "Europe/Madrid" }`
Response `200`: usuario actualizado.

### 4.3 Mercado de criptomonedas (`/api/cryptos`)

Todos requieren sesión 🔒. El backend cachea la respuesta de CoinGecko ~60s por combinación de parámetros.

#### `GET /api/cryptos?page=1&perPage=50&currency=usd`
Response `200`:
```json
{
  "page": 1,
  "perPage": 50,
  "items": [
    {
      "coinId": "bitcoin",
      "symbol": "btc",
      "name": "Bitcoin",
      "image": "https://...",
      "currentPrice": 64230.12,
      "priceChangePercentage24h": 2.35,
      "sparkline7d": [63000, 63500, ...]
    }
  ]
}
```
Errores: `502` CoinGecko no disponible (sin fallback a caché antigua) · `429` rate limit propio superado.

#### `GET /api/cryptos/{coinId}?currency=usd`
Response `200`: detalle individual (mismo shape que un item de la lista, con datos ampliados si aplica).
Errores: `404` coinId no existe en CoinGecko.

#### `GET /api/cryptos/search?q=bit`
Response `200`:
```json
{ "results": [ { "coinId": "bitcoin", "symbol": "btc", "name": "Bitcoin" } ] }
```

### 4.4 Watchlist (`/api/watchlist`) 🔒

#### `GET /api/watchlist`
Response `200`: `{ "items": [ { "coinId": "bitcoin", "addedAt": "2026-08-10T09:00:00Z" } ] }`

#### `POST /api/watchlist`
Request: `{ "coinId": "bitcoin" }` → Response `201`. Idempotente: si ya existe, `200` sin duplicar.
Errores: `404` coinId no válido en CoinGecko.

#### `DELETE /api/watchlist/{coinId}`
Response `204`.

### 4.5 Notas (`/api/notes`) 🔒

#### `GET /api/notes?coinId=&type=&tag=&page=1`
Response `200`:
```json
{
  "page": 1,
  "items": [
    {
      "id": "uuid",
      "title": "BTC rompe resistencia",
      "content": "Creo que...",
      "type": "PREDICCION",
      "coinIds": ["bitcoin"],
      "tags": ["corto-plazo"],
      "createdAt": "2026-08-15T12:00:00Z",
      "updatedAt": "2026-08-15T12:00:00Z"
    }
  ]
}
```

#### `POST /api/notes`
Request:
```json
{
  "title": "BTC vs ETH",
  "content": "Comparativa de fundamentales...",
  "type": "OBSERVACION",
  "coinIds": ["bitcoin", "ethereum"],
  "tags": ["comparativa", "largo-plazo"]
}
```
Response `201`: nota creada (mismo shape que GET). Validación: `title` requerido (≤100), `content` requerido (≤2000, sanitizado), `type` uno de los 4 valores, `coinIds` mínimo 1 elemento válido.

#### `GET /api/notes/{id}`
Response `200`: nota. Error `404` si no existe o no pertenece al usuario.

#### `PUT /api/notes/{id}`
Request: mismo shape que POST (reemplazo completo). Response `200`.

#### `DELETE /api/notes/{id}`
Response `204`.

#### `GET /api/cryptos/{coinId}/notes` 🔒
Notas propias asociadas a esa cripto concreta (atajo para la ficha de detalle). Mismo shape que `GET /api/notes`.

---

## 5. Flujos de usuario principales

### 5.1 Registro y primer acceso
1. Usuario rellena email/password/nombre → `POST /auth/register`.
2. Redirección a login → `POST /auth/login` → se guardan `accessToken` (memoria) y `refreshToken` (storage seguro).
3. Frontend programa renovación silenciosa vía `POST /auth/refresh` antes de que expire el access token (a los ~25 min si expira en 30).
4. Tras 3-5 intentos fallidos consecutivos de login, la cuenta queda bloqueada temporalmente (`423`) y se informa al usuario del tiempo de espera.

### 5.2 Consulta de estadísticas de mercado
1. Al entrar al dashboard, el frontend pide `GET /api/cryptos?page=1&currency={baseCurrency del usuario}`.
2. La tabla se refresca automáticamente cada 60s (nueva llamada al mismo endpoint).
3. Si CoinGecko falla (`502`), se muestra un mensaje de error explícito ("Datos de mercado no disponibles en este momento") en lugar de datos desactualizados.
4. El usuario puede buscar una cripto fuera del Top N vía `GET /api/cryptos/search`, y navegar a su ficha de detalle.
5. Desde la tabla o la ficha, el usuario puede añadir/quitar una cripto de su watchlist con un icono de estrella (`POST`/`DELETE /api/watchlist`).

### 5.3 Creación de una nota
1. Desde la ficha de una cripto (o desde un botón general "Nueva nota"), el usuario abre el formulario.
2. Completa título, contenido, tipo (selector de los 4 fijos), tags libres, y selecciona una o varias criptomonedas asociadas (por defecto, la de la ficha desde la que partió).
3. Validación en cliente (longitudes, campos requeridos) antes de enviar; el backend revalida y sanitiza el `content`.
4. `POST /api/notes` → la nota aparece en el listado de notas de cada cripto asociada.

### 5.4 Gestión de notas propias
1. El usuario accede a "Mis notas", con filtros por tipo, tag o cripto (`GET /api/notes?...`).
2. Puede editar una nota existente (`PUT`) o eliminarla (`DELETE`) — sin confirmación en backend, con diálogo de confirmación en frontend por ser destructivo.

---

## 6. Requisitos no funcionales

- **Manejo de errores de API externa:** ante fallo o timeout de CoinGecko, el backend responde `502` con mensaje claro; no se sirve caché obsoleta como sustituto.
- **Caché:** respuestas de `/api/cryptos*` cacheadas en backend ~60s por combinación de parámetros, para reducir llamadas a CoinGecko y respetar su rate limit.
- **Rate limiting propio:** límite por usuario/IP en los endpoints de la API Spring Boot (ej. vía Bucket4j), respuesta `429` al superarlo.
- **Validación:** doble capa (React + Bean Validation en Spring Boot); el backend es la fuente de verdad.
- **Seguridad:** contraseñas con BCrypt; sanitización del `content` de notas contra XSS antes de persistir/renderizar; JWT firmado, access token de vida corta (15-30 min), refresh token con rotación y revocación.
- **Responsive:** soporte completo desktop/tablet/móvil.
- **Idioma y moneda:** interfaz en español; precios en USD o EUR según preferencia del usuario (`baseCurrency`).

---

## 7. Criterios de aceptación por funcionalidad

### Registro/Login
- [ ] No se puede registrar dos veces el mismo email (`409`).
- [ ] Password rechazada si no cumple longitud/complejidad mínima.
- [ ] Login correcto devuelve access + refresh token con `expiresIn` coherente con la config (15-30 min).
- [ ] Tras N intentos fallidos configurados, login devuelve `423` aunque la contraseña sea correcta, hasta que expire el bloqueo.
- [ ] Un access token expirado es rechazado (`401`) por endpoints protegidos.
- [ ] `POST /auth/refresh` con un refresh token ya usado (rotado) es rechazado.

### Tabla de estadísticas
- [ ] La tabla carga el Top N por capitalización al entrar, en la moneda base del usuario.
- [ ] Los datos se refrescan automáticamente cada 60s sin recargar la página.
- [ ] Si CoinGecko no responde, se muestra un mensaje de error explícito y no una tabla vacía silenciosa ni datos obsoletos sin aviso.
- [ ] La búsqueda encuentra criptos fuera del Top N por nombre/símbolo.

### Watchlist
- [ ] Añadir una cripto ya presente en la watchlist no crea duplicados.
- [ ] Quitar una cripto de la watchlist la elimina inmediatamente de la vista sin recargar.

### Notas
- [ ] Una nota no se puede crear sin al menos una cripto asociada válida.
- [ ] Una nota puede asociarse a 2+ criptomonedas y aparece en el listado de notas de cada una.
- [ ] El `type` de nota solo acepta los 4 valores fijos; cualquier otro valor es rechazado (`400`).
- [ ] Editar una nota actualiza `updatedAt` y los cambios se reflejan en el listado sin duplicar la nota.
- [ ] Borrar una nota la elimina físicamente y deja de aparecer en todas las criptos a las que estaba asociada.
- [ ] El contenido de una nota con HTML/script embebido se guarda y renderiza de forma segura (sin ejecutar script).
- [ ] El filtro de notas por tipo, tag o cripto devuelve solo resultados coincidentes del usuario autenticado (nunca notas de otros usuarios).

### Seguridad y NFRs
- [ ] Un usuario no puede leer, editar ni borrar notas o watchlist de otro usuario (verificado por `userId` del token, no por parámetro de request).
- [ ] Superar el rate limit propio de un endpoint devuelve `429` con mensaje claro.
- [ ] La app es usable (sin overflow ni elementos rotos) en viewport de 375px de ancho (móvil).

---

## 8. Estado de implementación (MVP)

Resumen de qué parte del diseño descrito arriba está realmente construida a fecha
de esta revisión. Detalle issue por issue en [`BACKLOG.md`](BACKLOG.md).

### Implementado y verificado
- **AUTH-1 / AUTH-2** — Registro y login. Contraseña con BCrypt, access token JWT
  firmado (30 min por defecto). El login también emite un refresh token (opaco,
  hasheado con SHA-256 antes de persistir), pero **aún no hay endpoint
  `POST /api/auth/refresh` que lo consuma** — se emite pero no se usa todavía
  (AUTH-3 pendiente).
- **STATS-1** — Tabla de mercado vía proxy a CoinGecko, con caché de 60s en
  backend (Caffeine). El refresco automático cada 60s en frontend (STATS-2) y el
  selector de moneda (STATS-3) siguen pendientes.
- **NOTES-1 / NOTES-5** — Crear y borrar notas, asociables a varias criptomonedas,
  con tipo fijo, tags y contenido escapado (HTML entities) contra XSS. Editar
  (NOTES-4), filtrar (NOTES-3) y verlas desde la ficha de una cripto (NOTES-2)
  siguen pendientes — no existe aún ficha de detalle de cripto (STATS-5).

### Modelado pero no activo
- El modelo `User` ya tiene `failedLoginAttempts` y `lockedUntil` (según §3.1),
  pero **el login no los usa todavía**: no hay bloqueo tras intentos fallidos
  (AUTH-4). El frontend tiene el manejo de la respuesta `423` preparado, a la
  espera de que el backend la emita.

### No iniciado
Watchlist (WATCH-1), rate limiting propio (RATE-1), perfil editable (AUTH-6),
logout que revoque el refresh token (AUTH-5), búsqueda y ficha de detalle de
criptomonedas (STATS-4/STATS-5). Todo el roadmap de iteración 2 (§2.2) sigue sin
empezar.

### Decisiones tomadas durante la implementación (no cambian el diseño, sí el cómo)
- El backend corre sobre una versión de Spring Boot que reestructuró varios
  módulos internamente (Jackson 3, autoconfiguración de tests, caché) — el
  detalle técnico está en `informe_de_errores.md` (no versionado en el repo).
- El frontend migró a **pnpm** con `ignore-scripts=true` como refuerzo de
  seguridad de la cadena de suministro (ningún paquete actual necesita scripts
  de instalación).
- CORS se amplió más allá de `localhost` para aceptar orígenes de red local
  (`192.168.*.*`, `10.*.*.*`), pensado para pruebas desde el móvil en la misma
  red — no es una configuración de producción.
