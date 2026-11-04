# Changelog

Todos los cambios relevantes de este proyecto se documentan aquí. El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el proyecto usa [versionado semántico](https://semver.org/lang/es/).

## [0.2.0] - 2026-11-03

Segunda etapa: reservas, con cero dobles reservas bajo concurrencia.

### Agregado
- CRUD de horarios de atención (`/negocios/{negocioId}/horarios`), con turnos partidos el mismo día y validación de solapes.
- Disponibilidad por recurso: `GET /negocios/{negocioId}/recursos/{recursoId}/disponibilidad`. Calcula los turnos libres en la zona horaria del negocio, en rejilla de 15 minutos, sin solaparse con reservas confirmadas ni empezar en el pasado, y maneja el cambio de hora.
- Reservas: crear, obtener y listar (`/negocios/{negocioId}/reservas`). El inicio debe ser un turno que ofrece la disponibilidad: 400 si no lo es, 409 si está ocupado o el recurso o el servicio están inactivos.
- Prevención de dobles reservas: restricción de exclusión de PostgreSQL (`EXCLUDE USING gist`) sobre las reservas confirmadas de un recurso, más bloqueo de la fila del recurso al reservar. Una prueba de 50 hilos al mismo turno deja una sola reserva y 49 respuestas 409.
- Cancelar y reprogramar reservas (`.../cancelar` y `.../reprogramar`) con un mínimo de 2 horas de antelación, reprogramación a otro recurso y bloqueos en orden fijo.
- Documento `docs/concurrencia.md` con las mediciones, las alternativas consideradas y las limitaciones.
- Pruebas: de 34 a 93, entre ellas las de concurrencia con hilos reales (crear y reprogramar).

### Cambios
- Migración `V5` con la extensión `btree_gist` y la restricción `ex_reserva_recurso_solape`.

### Pendiente para próximas versiones
- Usuarios, JWT y roles; los campos `propietario_id` y `cliente_id` pasarán a ser obligatorios (Etapa 3).
- Mensajes de error unificados con RFC 7807: hoy las respuestas de error no incluyen el mensaje (Etapa 3).
- Recordatorios asíncronos y caché de disponibilidad (Etapa 4).
- Días u horas bloqueados, paginación y filtros (Etapas 5 y 8).

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

[0.2.0]: https://github.com/SrNe0/reservas-app/releases/tag/v0.2.0
[0.1.0]: https://github.com/SrNe0/reservas-app/releases/tag/v0.1.0
