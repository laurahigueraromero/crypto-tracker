# Crypto Tracker

Aplicación web de seguimiento de criptomonedas: registro/login con JWT, tabla de
mercado en vivo (vía CoinGecko) y notas personales asociadas a una o varias
criptomonedas. Backend en Spring Boot, frontend en React.

Desarrollado siguiendo **Spec-Driven Development**: PRD → backlog en vertical
slices → issues de GitHub con dependencias → implementación issue a issue con
tests y verificación real contra el servidor en marcha.

## Stack

- **Backend:** Spring Boot 4.1 (Java 21, Maven), Spring Security, Spring Data JPA,
  PostgreSQL, JJWT, Caffeine (caché), RestClient.
- **Frontend:** React 19 + Vite, React Router, Axios, pnpm.
- **Infraestructura:** Docker Compose (PostgreSQL), GitHub Issues + Project (Kanban)
  para la gestión del backlog.

## Funcionalidades implementadas

- Registro e inicio de sesión con contraseña hasheada (BCrypt) y token de acceso
  JWT firmado (expiración configurable, 30 min por defecto).
- Tabla de mercado con los datos principales de CoinGecko (precio, variación 24h,
  sparkline de 7 días), cacheada ~60s en el backend para no saturar la API externa.
- Creación, listado y borrado de notas personales, asociables a una o varias
  criptomonedas, con tipo fijo (predicción / motivo de compra / motivo de venta /
  observación), tags libres y contenido protegido frente a XSS (escapado de HTML
  antes de persistir).
- Diseño responsive: tabla en escritorio, tarjetas apiladas en móvil; modo claro/
  oscuro con paleta inspirada en GitHub.

### Fuera del MVP actual (backlog abierto)

Renovación de sesión (refresh token en uso), bloqueo de cuenta tras intentos
fallidos, perfil de usuario editable, selector de moneda, búsqueda y ficha de
detalle de criptomonedas, watchlist, edición/filtrado de notas y rate limiting
propio. El estado y las dependencias de cada pieza están en
[`BACKLOG.md`](BACKLOG.md) y en las [issues del repositorio](../../issues).

## Documentación del proyecto

- [`PRD.md`](PRD.md) — especificación funcional y técnica completa.
- [`BACKLOG.md`](BACKLOG.md) — backlog derivado del PRD en vertical slices, con
  dependencias y cadena crítica.

## Arrancar en local

Requisitos: Java 21+, Node 20+, pnpm, Docker.

```bash
# 1. Variables de entorno (la del frontend es opcional: sin ella, la app
#    detecta automáticamente la URL del backend a partir del host desde el
#    que se accede)
cp .env.example .env
cp frontend/.env.example frontend/.env

# 2. Base de datos
docker compose up -d

# 3. Backend (puerto 8080)
cd backend
./mvnw spring-boot:run

# 4. Frontend (puerto 5173), en otra terminal
cd frontend
pnpm install
pnpm run dev
```

## API

Todos los endpoints bajo `/api`. Los marcados 🔒 requieren cabecera
`Authorization: Bearer <token>`.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/register` | Registro de usuario |
| POST | `/api/auth/login` | Login, devuelve `accessToken` + `refreshToken` |
| GET 🔒 | `/api/cryptos` | Tabla de mercado (`page`, `perPage`, `currency`) |
| POST 🔒 | `/api/notes` | Crear nota |
| GET 🔒 | `/api/notes` | Listar mis notas |
| DELETE 🔒 | `/api/notes/{id}` | Borrar una nota propia |

## Tests

```bash
cd backend
./mvnw test
```

20 tests de integración (MockMvc + H2) cubriendo registro, login, autenticación
JWT en rutas protegidas, tabla de mercado (con caché mockeada) y CRUD de notas
(incluyendo aislamiento entre usuarios y sanitización de contenido).
