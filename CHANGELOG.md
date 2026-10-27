# Changelog

Todos los cambios relevantes de este proyecto se documentan aquí. El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [versionado semántico](https://semver.org/lang/es/).

## [0.1.0] - 2026-10-26

Primera etapa: la base del proyecto.

### Agregado
- Estructura del monorepo (`backend/`, `frontend/`, `infra/`, `docs/`), licencia MIT y README inicial.
- API con Spring Boot 4 y Java 21, con health check en `/actuator/health`.
- Docker Compose con la API y PostgreSQL 17, healthchecks y variables en `.env.example`.
- Esquema de base de datos con Flyway: usuarios, negocios, servicios, recursos, horarios y reservas, con claves foráneas compuestas que impiden mezclar datos de negocios distintos.
- CRUD de negocios (`/negocios`), servicios y recursos (`/negocios/{negocioId}/servicios` y `/recursos`), con validación, respuestas 201/204/400/404/409 y documentación en Swagger UI.
- Aislamiento por negocio: un servicio o recurso de otro negocio responde 404.
- 34 pruebas de integración con Testcontainers (PostgreSQL real).
- CI con GitHub Actions: compila y corre las pruebas en cada pull request y en cada push a `main`.
- Protección de `main`: solo se fusiona por pull request con el CI en verde.

### Pendiente para próximas versiones
- Cálculo de horarios disponibles y creación de reservas con prevención de dobles reservas (Etapa 2).
- Usuarios, JWT y roles; los campos `propietario_id` y `cliente_id` pasarán a ser obligatorios (Etapa 3).
- Mensajes de error unificados con RFC 7807 (Etapa 3).

[0.1.0]: https://github.com/SrNe0/reservas-app/releases/tag/v0.1.0
