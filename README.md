# reservas-app

[![CI](https://github.com/SrNe0/reservas-app/actions/workflows/ci.yml/badge.svg)](https://github.com/SrNe0/reservas-app/actions/workflows/ci.yml)

Aplicación web full stack para que pequeños negocios gestionen reservas y turnos en línea, y sus clientes reserven sin llamadas ni mensajes.

## El problema

Muchos negocios pequeños, como barberías, consultorios o canchas, todavía manejan su agenda por WhatsApp, llamadas o una libreta. Eso provoca turnos perdidos, dobles reservas cuando dos personas piden el mismo horario y clientes que no llegan porque nadie les recordó la cita. Además, el dueño no tiene datos para saber qué servicios se piden más o en qué horarios hay más demanda.

Construí reservas-app para resolver eso: cada negocio publica sus servicios y horarios, y los clientes reservan solo en los turnos que realmente están libres.

## Retos técnicos

- **Concurrencia:** garantizar cero dobles reservas cuando varias personas intentan tomar el mismo turno al mismo tiempo.
- **Multi-tenant:** varios negocios en un mismo sistema, cada uno con acceso únicamente a sus datos.
- **Asincronía:** recordatorios y tareas en segundo plano, fuera del ciclo de la petición.
- **Seguridad:** autenticación con JWT, roles (administrador, negocio, cliente) y límite de peticiones.
- **Observabilidad:** métricas, logs estructurados y dashboards para saber qué pasa en producción.
- **Orquestación:** despliegue en Kubernetes con autoescalado.

## Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Maven |
| Base de datos | PostgreSQL, Flyway |
| Caché y colas | Redis |
| Frontend | React, TypeScript, Vite, Tailwind CSS |
| Pruebas | JUnit 5, Testcontainers, Playwright, k6 |
| Observabilidad | Prometheus, Loki, Grafana |
| Infraestructura | Docker Compose, GitHub Actions, Kubernetes, Helm |

## Demo

Próximamente.

## Capturas

Próximamente.

## Arquitectura

Próximamente.

## Cómo correrlo

Requisitos: Docker con Docker Compose.

```
cp .env.example .env
docker compose up -d --build
```

En Windows con PowerShell o CMD usa `copy .env.example .env`. Edita el `.env` si quieres cambiar las contraseñas o los puertos.

- API: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- PostgreSQL: `127.0.0.1:5432` (usuario y base definidos en `.env`)

Para detener todo conservando los datos: `docker compose down`. Agregar `-v` borra también el volumen de PostgreSQL.

### Solo el backend, sin levantar la API en Docker

Requisitos: JDK 21 y la base de datos en marcha. No hace falta instalar Maven porque el proyecto incluye el Maven Wrapper.

```
docker compose up -d db
cd backend
SPRING_DATASOURCE_PASSWORD=<la de tu .env> ./mvnw spring-boot:run
```

En Windows usa `.\mvnw.cmd` y define la variable antes (`$env:SPRING_DATASOURCE_PASSWORD="..."` en PowerShell).

## API

La documentación interactiva está en Swagger UI: http://localhost:8080/swagger-ui/index.html

| Recurso | Rutas |
|---|---|
| Negocios | `/negocios`, `/negocios/{id}` |
| Servicios | `/negocios/{negocioId}/servicios`, `/negocios/{negocioId}/servicios/{id}` |
| Recursos (barbero, cancha, consultorio) | `/negocios/{negocioId}/recursos`, `/negocios/{negocioId}/recursos/{id}` |

Cada una admite crear (`POST`, responde 201 con `Location`), listar y obtener (`GET`), actualizar (`PUT`) y eliminar (`DELETE`, responde 204). Los errores usan 400 si los datos no son válidos, 404 si no existe y 409 si no se puede borrar porque tiene reservas asociadas.

## Pruebas

```
cd backend
./mvnw test
```

Las pruebas necesitan Docker en marcha: Testcontainers levanta un PostgreSQL temporal y lo destruye al terminar.

El mismo comando corre en GitHub Actions en cada pull request y en cada push a `main` (`.github/workflows/ci.yml`).

## Decisiones técnicas

- **Aislamiento por negocio desde la ruta:** servicios y recursos cuelgan de `/negocios/{negocioId}/...` y toda consulta busca por `(id, negocioId)`. Si pides un recurso con el negocio equivocado, responde 404 como si no existiera. Hay pruebas que lo verifican.
- **El esquema lo maneja Flyway, no Hibernate:** uso `ddl-auto=validate`, así Hibernate solo comprueba que las entidades coincidan con las migraciones.
- **Claves foráneas compuestas:** una reserva referencia `(servicio_id, negocio_id)` y `(recurso_id, negocio_id)`, de modo que la base de datos impide mezclar datos de negocios distintos.
- **Organización por funcionalidad:** el código está en paquetes `negocio/`, `servicio/` y `recurso/`, no separado por capas técnicas.
- **DTO como `record`:** los objetos de entrada y salida son inmutables y las entidades JPA quedan fuera de la API.

## Roadmap

| Etapa | Versión | Estado |
|---|---|---|
| 1. Base | v0.1.0 | 🟡 En curso |
| 2. Reservas | v0.2.0 | ⬜ Pendiente |
| 3. Usuarios y seguridad | v0.3.0 | ⬜ Pendiente |
| 4. Asincronía | v0.4.0 | ⬜ Pendiente |
| 5. Calidad y documentación | v0.5.0 | ⬜ Pendiente |
| 6. Observabilidad y despliegue | v0.6.0 | ⬜ Pendiente |
| 7. Frontend del cliente | v0.7.0 | ⬜ Pendiente |
| 8. Paneles de negocio y admin | v0.8.0 | ⬜ Pendiente |
| 9. Tiempo real y pulido | v1.0.0 | ⬜ Pendiente |
| 10. Kubernetes | v1.1.0 | ⬜ Pendiente |

## Licencia

MIT. Ver [LICENSE](LICENSE).
