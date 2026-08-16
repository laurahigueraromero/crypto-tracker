# Backlog — Crypto Tracker

Derivado de `PRD.md` v1.0. Organizado en vertical slices: cada issue es una funcionalidad
de extremo a extremo (modelo + endpoint + seguridad + UI mínima), demostrable por sí sola.

---

## Milestone: MVP

### AUTH-1 — Como visitante, puedo registrarme con email y contraseña
**Descripción:** Alta de cuenta nueva. Base de todo lo demás: sin usuario no hay sesión.
**Criterios de aceptación:**
- Given un email no registrado, When envío el formulario con password válida, Then recibo `201` y puedo iniciar sesión después.
- Given un email ya registrado, When intento registrarme de nuevo, Then recibo `409` y un mensaje claro en el formulario.
- Given una password que no cumple el mínimo (8+ caracteres, 1 mayúscula, 1 número), When la envío, Then recibo `400` con el motivo, sin llegar a crear el usuario.
**Definición de hecho:**
- Entidad `User`, `passwordHash` con BCrypt, endpoint `POST /api/auth/register`.
- Validación en frontend (feedback inmediato) y backend (fuente de verdad).
- Formulario de registro responsive, con mensajes de error visibles.
- Test de integración: registro exitoso, email duplicado, password inválida.
**Labels:** `auth`, `mvp`
**Estimación:** S
**Bloqueado por:** —

---

### AUTH-2 — Como usuario registrado, puedo iniciar sesión y quedar autenticado
**Descripción:** Login con emisión de access + refresh token; el frontend persiste la sesión y protege rutas.
**Criterios de aceptación:**
- Given credenciales correctas, When hago login, Then recibo `accessToken`, `refreshToken` y `expiresIn`, y accedo a rutas protegidas.
- Given credenciales incorrectas, When hago login, Then recibo `401` sin revelar si el email existe o no.
- Given que no he iniciado sesión, When intento acceder a una ruta protegida del frontend, Then soy redirigido a login.
**Definición de hecho:**
- `POST /api/auth/login`, entidad `RefreshToken` (tokenHash, expiresAt, revoked).
- Access token JWT firmado, expiración 15-30 min configurable.
- Frontend: guarda tokens, interceptor añade `Authorization: Bearer`, rutas protegidas.
- Tests: login correcto, credenciales inválidas, acceso a endpoint protegido sin token (`401`).
**Labels:** `auth`, `mvp`
**Estimación:** L
**Bloqueado por:** AUTH-1

---

### AUTH-3 — Como usuario, mi sesión se renueva sola sin tener que volver a loguearme cada 30 min
**Descripción:** Renovación silenciosa vía refresh token con rotación, para no interrumpir el uso normal de la app.
**Criterios de aceptación:**
- Given un refresh token válido, When el access token está a punto de expirar, Then el frontend lo renueva automáticamente sin acción del usuario.
- Given un refresh token ya usado (rotado), When se intenta reutilizar, Then el backend lo rechaza (`401`).
- Given un refresh token expirado, When se usa, Then el usuario es redirigido a login.
**Definición de hecho:**
- `POST /api/auth/refresh` con rotación (revoca el token anterior, emite uno nuevo).
- Frontend: temporizador/interceptor que dispara el refresh antes de expirar.
- Test: rotación correcta, reintento de token ya usado rechazado.
**Labels:** `auth`, `mvp`
**Estimación:** M
**Bloqueado por:** AUTH-2

---

### AUTH-4 — Como usuario, mi cuenta se bloquea temporalmente tras varios intentos fallidos de login
**Descripción:** Mitigación básica de fuerza bruta sobre el login.
**Criterios de aceptación:**
- Given 5 intentos fallidos consecutivos, When intento un 6º login (aunque la password sea correcta), Then recibo `423` con el tiempo de espera restante.
- Given una cuenta bloqueada, When pasa el tiempo de bloqueo, Then puedo volver a intentar login con normalidad.
**Definición de hecho:**
- Campos `failedLoginAttempts`, `lockedUntil` en `User`.
- Mensaje claro en frontend explicando el bloqueo temporal.
- Test: bloqueo tras N intentos, desbloqueo tras expirar `lockedUntil`.
**Labels:** `auth`, `security`, `mvp`
**Estimación:** S
**Bloqueado por:** AUTH-2

---

### AUTH-5 — Como usuario, puedo cerrar sesión
**Descripción:** Logout explícito que invalida el refresh token activo.
**Criterios de aceptación:**
- Given una sesión activa, When pulso "Cerrar sesión", Then el refresh token queda revocado y no puede reutilizarse.
- Given que cierro sesión, When intento acceder a una ruta protegida, Then soy redirigido a login.
**Definición de hecho:**
- `POST /api/auth/logout` marca `revoked=true`.
- Frontend limpia tokens locales y redirige.
**Labels:** `auth`, `mvp`
**Estimación:** S
**Bloqueado por:** AUTH-2

---

### AUTH-6 — Como usuario, puedo ver y editar mi perfil (nombre, avatar, moneda base, zona horaria)
**Descripción:** Gestión básica de perfil, necesaria para que exista `baseCurrency` (usado por la tabla de estadísticas).
**Criterios de aceptación:**
- Given estoy autenticado, When abro "Mi perfil", Then veo mis datos actuales.
- Given cambio mi moneda base a EUR, When guardo, Then el cambio persiste y se refleja en próximas consultas.
**Definición de hecho:**
- `GET/PUT /api/users/me`.
- Formulario de edición con validación básica.
- Test: actualización correcta, valores no permitidos en `baseCurrency` rechazados.
**Labels:** `auth`, `profile`, `mvp`
**Estimación:** M
**Bloqueado por:** AUTH-2

---

### STATS-1 — Como usuario autenticado, puedo ver la tabla de las criptomonedas principales por capitalización
**Descripción:** Slice central de estadísticas: backend como proxy con caché frente a CoinGecko, tabla paginada en el frontend.
**Criterios de aceptación:**
- Given estoy autenticado, When abro el dashboard, Then veo el Top N de criptos con ranking, logo, nombre, símbolo, precio y variación 24h.
- Given hago dos peticiones en menos de 60s, When se consulta el backend, Then la segunda se sirve desde caché (no se duplica la llamada a CoinGecko).
- Given pido la página 2, When la solicito, Then recibo el siguiente bloque de resultados.
**Definición de hecho:**
- `GET /api/cryptos?page=&perPage=&currency=`, caché backend ~60s por combinación de parámetros.
- Tabla responsive en frontend con paginación.
- Test: respuesta paginada correcta, caché evita llamada duplicada dentro de la ventana.
**Labels:** `stats`, `mvp`
**Estimación:** L
**Bloqueado por:** AUTH-2

---

### STATS-2 — Como usuario, la tabla se actualiza sola cada 60 segundos
**Descripción:** Refresco automático sin recargar la página.
**Criterios de aceptación:**
- Given tengo el dashboard abierto, When pasan 60s, Then los precios se actualizan sin recargar la página ni perder el scroll/página actual.
**Definición de hecho:**
- Polling en frontend cada 60s, cancelable al salir de la vista.
**Labels:** `stats`, `mvp`
**Estimación:** S
**Bloqueado por:** STATS-1

---

### STATS-3 — Como usuario, puedo ver los precios en USD o EUR según mi preferencia
**Descripción:** El selector de moneda de perfil se refleja en los datos de mercado mostrados.
**Criterios de aceptación:**
- Given mi `baseCurrency` es EUR, When cargo la tabla, Then los precios se muestran en euros.
- Given cambio de moneda desde el propio dashboard, When confirmo, Then la tabla se recarga en la nueva moneda y la preferencia queda guardada en mi perfil.
**Definición de hecho:**
- Parámetro `currency` en `GET /api/cryptos` conectado a `baseCurrency` del usuario.
- Selector de moneda en UI, sincronizado con AUTH-6.
**Labels:** `stats`, `mvp`
**Estimación:** M
**Bloqueado por:** STATS-1, AUTH-6

---

### STATS-4 — Como usuario, puedo buscar cualquier criptomoneda aunque no esté en el Top N
**Descripción:** Búsqueda por nombre/símbolo sobre todo el catálogo de CoinGecko.
**Criterios de aceptación:**
- Given escribo "sol" en el buscador, When se ejecuta la búsqueda, Then veo coincidencias como "Solana" aunque no esté en el Top N visible.
- Given no hay coincidencias, When busco un término inexistente, Then veo un estado vacío claro (no un error).
**Definición de hecho:**
- `GET /api/cryptos/search?q=`.
- Componente de búsqueda con debounce en frontend.
**Labels:** `stats`, `mvp`
**Estimación:** M
**Bloqueado por:** STATS-1

---

### STATS-5 — Como usuario, puedo abrir la ficha de detalle de una criptomoneda
**Descripción:** Vista individual, punto de entrada para watchlist y notas asociadas a esa cripto.
**Criterios de aceptación:**
- Given estoy en la tabla o en resultados de búsqueda, When pulso sobre una cripto, Then accedo a su ficha con sus datos actuales.
- Given el `coinId` no existe, When se solicita su ficha, Then recibo `404` y el frontend muestra un estado "no encontrado".
**Definición de hecho:**
- `GET /api/cryptos/{coinId}?currency=`.
- Página de detalle responsive.
**Labels:** `stats`, `mvp`
**Estimación:** M
**Bloqueado por:** STATS-1

---

### STATS-6 — Como usuario, veo un aviso claro si los datos de mercado no están disponibles
**Descripción:** Manejo explícito del fallo de CoinGecko (sin fallback silencioso a datos antiguos, según PRD §6).
**Criterios de aceptación:**
- Given CoinGecko no responde o supera el timeout, When cargo la tabla o una ficha, Then veo un mensaje explícito de "datos no disponibles", no una tabla vacía ni un error genérico.
**Definición de hecho:**
- Backend devuelve `502` con mensaje estructurado ante fallo upstream.
- Frontend muestra estado de error dedicado (no un crash ni un spinner infinito).
**Labels:** `stats`, `error-handling`, `mvp`
**Estimación:** S
**Bloqueado por:** STATS-1

---

### RATE-1 — Como operador del sistema, la API rechaza abuso de peticiones por usuario/IP
**Descripción:** Rate limiting propio para proteger la API de Spring Boot, independiente del límite de CoinGecko.
**Criterios de aceptación:**
- Given supero el límite configurado de peticiones en la ventana definida, When hago una petición adicional, Then recibo `429` con un mensaje claro.
- Given estoy dentro del límite, When uso la app con normalidad, Then no noto ninguna restricción.
**Definición de hecho:**
- Rate limiting (ej. Bucket4j) aplicado por usuario autenticado y por IP.
- Test: petición número N+1 dentro de la ventana devuelve `429`.
**Labels:** `security`, `mvp`
**Estimación:** M
**Bloqueado por:** AUTH-2, STATS-1

---

### WATCH-1 — Como usuario autenticado, puedo añadir, ver y quitar criptomonedas de mi watchlist
**Descripción:** Seguimiento personal independiente de las notas (PRD §2.1, §3.1).
**Criterios de aceptación:**
- Given estoy en la tabla o ficha de una cripto, When pulso "seguir", Then aparece en mi watchlist y el icono refleja el estado sin recargar.
- Given una cripto ya está en mi watchlist, When intento añadirla de nuevo, Then no se duplica (idempotente).
- Given tengo criptos en watchlist, When abro "Mi watchlist", Then las veo listadas.
- Given quito una cripto de mi watchlist, When confirmo, Then desaparece inmediatamente de la vista.
**Definición de hecho:**
- Entidad `WatchlistItem` (único por `userId`+`coinId`), `GET/POST /api/watchlist`, `DELETE /api/watchlist/{coinId}`.
- Icono de estrella en tabla/ficha + vista "Mi watchlist".
- Test: alta, alta duplicada (idempotente), baja.
**Labels:** `watchlist`, `mvp`
**Estimación:** M
**Bloqueado por:** AUTH-2, STATS-1

---

### NOTES-1 — Como usuario autenticado, puedo crear una nota asociada a una o varias criptomonedas
**Descripción:** Slice central de notas: creación con tipo, tags y asociación N:M a criptos.
**Criterios de aceptación:**
- Given completo título, contenido, tipo y al menos una cripto, When guardo, Then la nota se crea y aparece asociada a cada cripto seleccionada.
- Given no selecciono ninguna cripto, When intento guardar, Then recibo `400` y no se crea la nota.
- Given el tipo enviado no es uno de los 4 permitidos, When se envía, Then recibo `400`.
- Given incluyo `<script>` en el contenido, When la nota se guarda y se renderiza después, Then el script no se ejecuta (contenido sanitizado).
**Definición de hecho:**
- Entidades `Note`, `NoteCoin`, `NoteTag`; `POST /api/notes`.
- Validación de longitudes (título ≤100, contenido ≤2000) en frontend y backend.
- Sanitización anti-XSS del contenido antes de persistir/renderizar.
- Formulario de creación con selector multi-cripto, tipo (4 opciones fijas) y tags libres.
- Test: creación válida, sin coinIds, tipo inválido, contenido con HTML malicioso.
**Labels:** `notes`, `mvp`
**Estimación:** L
**Bloqueado por:** AUTH-2, STATS-1

---

### NOTES-2 — Como usuario, veo mis notas asociadas al abrir la ficha de una criptomoneda
**Descripción:** Las notas se consultan en el contexto natural: la ficha de la cripto a la que pertenecen.
**Criterios de aceptación:**
- Given tengo notas asociadas a "bitcoin", When abro su ficha, Then las veo listadas (título, tipo, fecha).
- Given no tengo notas para esa cripto, When abro su ficha, Then veo un estado vacío invitando a crear una.
**Definición de hecho:**
- `GET /api/cryptos/{coinId}/notes`.
- Sección de notas integrada en la ficha de detalle.
**Labels:** `notes`, `mvp`
**Estimación:** S
**Bloqueado por:** NOTES-1, STATS-5

---

### NOTES-3 — Como usuario, puedo ver y filtrar todas mis notas en una sección "Mis notas"
**Descripción:** Vista global de notas propias, con filtro por tipo, tag o cripto (PRD §2.1).
**Criterios de aceptación:**
- Given tengo notas de varios tipos, When filtro por tipo "PREDICCION", Then solo veo esas.
- Given filtro por un tag, When aplico el filtro, Then solo veo notas con ese tag.
- Given otro usuario tiene notas, When yo consulto "Mis notas", Then nunca veo las suyas.
**Definición de hecho:**
- `GET /api/notes?coinId=&type=&tag=&page=`, filtrado siempre por el usuario del token (nunca por parámetro de request).
- Vista "Mis notas" con filtros combinables.
- Test: filtro por tipo, por tag, por cripto, y aislamiento entre usuarios.
**Labels:** `notes`, `mvp`
**Estimación:** M
**Bloqueado por:** NOTES-1

---

### NOTES-4 — Como usuario, puedo editar una nota existente
**Descripción:** Corrección de una nota ya creada (título, contenido, tipo, tags, criptos asociadas).
**Criterios de aceptación:**
- Given soy el propietario de una nota, When la edito y guardo, Then los cambios se reflejan y `updatedAt` se actualiza.
- Given intento editar una nota de otro usuario (manipulando el id), When lo intento, Then recibo `404`/`403`.
**Definición de hecho:**
- `PUT /api/notes/{id}` con las mismas validaciones que la creación.
- Test: edición propia correcta, intento de edición ajena rechazado.
**Labels:** `notes`, `mvp`
**Estimación:** S
**Bloqueado por:** NOTES-1

---

### NOTES-5 — Como usuario, puedo eliminar una nota
**Descripción:** Borrado físico definitivo, con confirmación en UI por ser destructivo.
**Criterios de aceptación:**
- Given soy el propietario, When confirmo el borrado, Then la nota desaparece de todas las criptos a las que estaba asociada.
- Given intento borrar una nota de otro usuario, When lo intento, Then recibo `404`/`403`.
**Definición de hecho:**
- `DELETE /api/notes/{id}` (borrado físico, cascada sobre `NoteCoin`/`NoteTag`).
- Diálogo de confirmación en frontend.
**Labels:** `notes`, `mvp`
**Estimación:** S
**Bloqueado por:** NOTES-1

---

## Milestone: Iteración 2+

### ITER2-1 — Como usuario, puedo definir un precio objetivo y recibir una alerta al alcanzarlo
**Descripción:** Alertas de precio (PRD §2.2, roadmap explícito).
**Criterios de aceptación:** Given defino un precio objetivo para una cripto, When el mercado lo alcanza, Then recibo una notificación.
**Definición de hecho:** Job periódico de comprobación de precios, entidad de alerta, mecanismo de notificación (definir canal: email/in-app).
**Labels:** `alerts`, `iteration-2`
**Estimación:** L
**Bloqueado por:** STATS-1, WATCH-1 (conceptualmente relacionado, no bloqueante estricto)

---

### ITER2-2 — Como administrador, puedo gestionar usuarios de la plataforma
**Descripción:** Rol admin reservado en el modelo desde el MVP, sin funcionalidad hasta esta fase (PRD §2.2).
**Criterios de aceptación:** Given tengo rol ADMIN, When accedo al panel, Then puedo listar/desactivar usuarios. Given tengo rol USER, When intento acceder, Then recibo `403`.
**Definición de hecho:** Endpoints protegidos por rol, panel de administración en frontend.
**Labels:** `admin`, `iteration-2`
**Estimación:** L
**Bloqueado por:** AUTH-2

---

### ITER2-3 — Como usuario, si CoinGecko falla veo los últimos datos conocidos con un aviso, en vez de un error
**Descripción:** Mejora de STATS-6: fallback a caché "stale" en lugar de error explícito.
**Criterios de aceptación:** Given CoinGecko falla y hay caché previa, When cargo la tabla, Then veo esos datos con una marca de "actualizado hace X min".
**Definición de hecho:** Backend sirve la última caché válida ante fallo upstream; UI distingue datos "en vivo" vs "obsoletos".
**Labels:** `stats`, `iteration-2`
**Estimación:** S
**Bloqueado por:** STATS-6

---

### ITER2-4 — Como usuario, puedo ver un gráfico histórico de precio en la ficha de una cripto
**Descripción:** Gráfico de velas/línea histórico, más allá del sparkline de 7 días de la tabla.
**Criterios de aceptación:** Given abro la ficha de una cripto, When selecciono un rango (24h/7d/30d/1a), Then veo el gráfico correspondiente.
**Definición de hecho:** Endpoint de histórico (proxy a CoinGecko `market_chart`), componente de gráfico en frontend.
**Labels:** `stats`, `iteration-2`
**Estimación:** M
**Bloqueado por:** STATS-5

---

### ITER2-5 — Como usuario, puedo exportar mis notas a CSV/PDF
**Descripción:** Exportación de notas propias (PRD §2.2).
**Criterios de aceptación:** Given tengo notas creadas, When pulso "Exportar", Then descargo un archivo con mis notas filtradas según la vista actual.
**Definición de hecho:** Endpoint de exportación, generación de CSV/PDF en backend.
**Labels:** `notes`, `iteration-2`
**Estimación:** M
**Bloqueado por:** NOTES-3

---

### ITER2-6 — Como usuario, puedo simular un portfolio y ver ganancias/pérdidas
**Descripción:** Cartera simulada con cantidades reales de cripto (PRD §2.2).
**Criterios de aceptación:** Given registro una posición (cripto, cantidad, precio de compra), When consulto mi portfolio, Then veo la ganancia/pérdida actual calculada con el precio de mercado.
**Definición de hecho:** Nueva entidad de posiciones, cálculo de P&L, vista de portfolio.
**Labels:** `portfolio`, `iteration-2`
**Estimación:** L
**Bloqueado por:** STATS-1, AUTH-2

---

### ITER2-7 — Como usuario, puedo compartir una nota públicamente
**Descripción:** Las notas son privadas por defecto; esta slice permite hacerlas públicas o compartirlas (PRD §2.2).
**Criterios de aceptación:** Given marco una nota como pública, When comparto su enlace, Then otra persona puede verla sin autenticarse (solo lectura).
**Definición de hecho:** Campo de visibilidad en `Note`, endpoint público de solo lectura, control de que el resto de notas sigan siendo privadas por defecto.
**Labels:** `notes`, `iteration-2`
**Estimación:** M
**Bloqueado por:** NOTES-1

---

### ITER2-8 — Como usuario, debo verificar mi email antes de poder iniciar sesión
**Descripción:** Verificación de email tras el registro (PRD §2.2).
**Criterios de aceptación:** Given me registro, When no he verificado el email, Then no puedo iniciar sesión hasta hacerlo. Given verifico mi email vía el enlace recibido, When inicio sesión después, Then funciona con normalidad.
**Definición de hecho:** Campo `emailVerified`, envío de email de verificación, endpoint de confirmación.
**Labels:** `auth`, `iteration-2`
**Estimación:** M
**Bloqueado por:** AUTH-1

---

## Cadena crítica del MVP

Camino mínimo de issues bloqueantes para tener algo funcional de punta a punta
(registro → login → ver mercado → crear una nota sobre una cripto real):

```
AUTH-1 → bloquea → AUTH-2
AUTH-2 → bloquea → STATS-1
AUTH-2 → bloquea → RATE-1
STATS-1 → bloquea → NOTES-1
STATS-1 → bloquea → WATCH-1
STATS-1 → bloquea → STATS-2, STATS-4, STATS-5, STATS-6, RATE-1, STATS-3
AUTH-6 → bloquea → STATS-3
NOTES-1 → bloquea → NOTES-2 (también requiere STATS-5), NOTES-3, NOTES-4, NOTES-5
AUTH-2 → bloquea → AUTH-3, AUTH-4, AUTH-5, AUTH-6
```

**Cadena crítica mínima:** `AUTH-1 → AUTH-2 → STATS-1 → NOTES-1`
(4 issues; a partir de ahí, WATCH-1, el resto de STATS-* y NOTES-2/3/4/5 se pueden
paralelizar entre distintos desarrolladores sin pisarse).

No se han creado dependencias artificiales: por ejemplo, STATS-4 (búsqueda) y
STATS-5 (ficha de detalle) solo dependen de STATS-1, no entre sí, y pueden avanzar
en paralelo. Lo mismo ocurre entre AUTH-3/4/5/6 una vez existe AUTH-2, o entre
NOTES-3/4/5 una vez existe NOTES-1.
