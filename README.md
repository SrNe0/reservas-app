# reservas-app

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

Requisitos: JDK 21. No hace falta instalar Maven: el proyecto incluye el Maven Wrapper.

```
cd backend
./mvnw spring-boot:run
```

La API queda en http://localhost:8080 y el health check en http://localhost:8080/actuator/health. En Windows usa `.\mvnw.cmd` en lugar de `./mvnw`. Más adelante se podrá levantar todo con `docker compose up`.
## Pruebas

```
cd backend
./mvnw test
```
## Decisiones técnicas

Próximamente.

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
