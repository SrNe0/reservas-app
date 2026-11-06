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

`JWT_SECRET` es la clave con la que la API firma los tokens y es obligatoria: `docker compose` no arranca sin ella. El valor de `.env.example` es solo un ejemplo; genera el tuyo (mínimo 32 caracteres) y no lo subas nunca. En PowerShell:

```
$b = New-Object byte[] 48; [Security.Cryptography.RandomNumberGenerator]::Fill($b); [Convert]::ToBase64String($b)
```

En Linux, macOS o Git Bash: `openssl rand -base64 48`.

- API: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- PostgreSQL: `127.0.0.1:5432` (usuario y base definidos en `.env`)

Para detener todo conservando los datos: `docker compose down`. Agregar `-v` borra también el volumen de PostgreSQL.

### Solo el backend, sin levantar la API en Docker

Requisitos: JDK 21 y la base de datos en marcha. No hace falta instalar Maven porque el proyecto incluye el Maven Wrapper.

```
docker compose up -d db
cd backend
SPRING_DATASOURCE_PASSWORD=<la de tu .env> JWT_SECRET=<la de tu .env> ./mvnw spring-boot:run
```

En Windows usa `.\mvnw.cmd` y define las variables antes (`$env:SPRING_DATASOURCE_PASSWORD="..."` y `$env:JWT_SECRET="..."` en PowerShell).

## API

La documentación interactiva está en Swagger UI: http://localhost:8080/swagger-ui/index.html

| Recurso | Rutas |
|---|---|
| Negocios | `/negocios`, `/negocios/{id}` |
| Servicios | `/negocios/{negocioId}/servicios`, `/negocios/{negocioId}/servicios/{id}` |
| Recursos (barbero, cancha, consultorio) | `/negocios/{negocioId}/recursos`, `/negocios/{negocioId}/recursos/{id}` |
| Horarios de atención | `/negocios/{negocioId}/horarios`, `/negocios/{negocioId}/horarios/{id}` |
| Disponibilidad (solo `GET`) | `/negocios/{negocioId}/recursos/{recursoId}/disponibilidad?servicioId=N&fecha=AAAA-MM-DD` |
| Reservas | `/negocios/{negocioId}/reservas`, `/negocios/{negocioId}/reservas/{id}` (`POST`, `GET`; el listado admite `?recursoId=`) |
| Cancelar y reprogramar | `POST /negocios/{negocioId}/reservas/{id}/cancelar` y `.../reprogramar` |
| Registro de usuarios | `POST /auth/registro` |
| Inicio de sesión | `POST /auth/login` |

Cada una admite crear (`POST`, responde 201 con `Location`), listar y obtener (`GET`), actualizar (`PUT`) y eliminar (`DELETE`, responde 204). Los errores usan 400 si los datos no son válidos, 404 si no existe y 409 si no se puede borrar porque tiene reservas asociadas o, en los horarios, si el turno se solapa con otro del mismo día.

La disponibilidad devuelve los turnos libres de un recurso para un servicio en una fecha. La fecha se interpreta en la zona horaria del negocio y los turnos salen en UTC; empiezan cada 15 minutos desde la apertura de cada tramo del horario, no se solapan con reservas confirmadas ni empiezan en el pasado, y solo se consulta hasta 60 días hacia adelante. Es una consulta informativa: no reserva nada, la garantía contra dobles reservas se aplica al crear la reserva.

Para reservar envío el servicio, el recurso y el instante de inicio, que debe ser uno de los turnos que ofrece la disponibilidad: si no lo es (pasado, fuera del horario o fuera de la rejilla de 15 minutos) responde 400, y si el turno ya está ocupado o el recurso o el servicio están inactivos responde 409. El fin se calcula con la duración del servicio y el precio se copia del servicio al reservar.

Se puede cancelar o reprogramar una reserva hasta 2 horas antes de que empiece (con exactamente 2 horas todavía se permite); si falta menos, o ya estaba cancelada, responde 409. Reprogramar acepta otro recurso y aplica las mismas reglas que reservar sin contar la propia reserva como ocupada.

Para registrarse envío email, contraseña (de 8 a 72 caracteres) y nombre; la cuenta es de CLIENTE salvo que pida `"tipoDeCuenta": "NEGOCIO"`, y no se puede elegir ADMIN. La contraseña se guarda con BCrypt (hash con sal) y no se devuelve nunca; el email se guarda en minúsculas y es único sin distinguir mayúsculas (409 si ya existe).

Para iniciar sesión envío email y contraseña a `POST /auth/login` y recibo `{"accessToken": "...", "tipo": "Bearer", "expiraEn": 900}`. El access token es un JWT firmado con HS256 que dura 15 minutos (`JWT_EXPIRACION_MINUTOS`) y lleva el id del usuario y su rol. Se envía en la cabecera `Authorization: Bearer <token>`; en Swagger se pega con el botón «Authorize». Si el email no existe o la contraseña es incorrecta, la respuesta es la misma (401 «Credenciales incorrectas»), y en ambos casos se ejecuta BCrypt para que el tiempo de respuesta tampoco delate qué cuentas existen. Por ahora los endpoints existentes siguen abiertos: el token se valida y se identifica al usuario, pero exigirlo por rol llega en el siguiente paso de la etapa.

Los errores se devuelven en formato Problem Details (RFC 9457) con `Content-Type: application/problem+json`:

```json
{
  "title": "Datos inválidos",
  "status": 400,
  "detail": "Uno o más campos de la solicitud no son válidos.",
  "instance": "/negocios",
  "errores": [{"campo": "nombre", "mensaje": "must not be blank"}]
}
```

El `detail` explica la causa (por ejemplo «El turno ya está ocupado por otra reserva»). Los errores inesperados responden 500 con un mensaje genérico, sin detalles internos.

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
- **Cero dobles reservas:** una restricción de exclusión de PostgreSQL (`EXCLUDE USING gist` sobre el recurso y el rango `[inicio, fin)` de las reservas confirmadas) impide que dos reservas del mismo recurso se solapen, incluso si llegan a la vez o si el código tiene un fallo. Además bloqueo la fila del recurso al reservar para que las peticiones concurrentes hagan cola: solo con la restricción, una ráfaga sobre el mismo turno provocaba deadlocks que agotaban el pool de conexiones. Lo verifico con una prueba de 50 hilos sobre el mismo turno. Las mediciones, las alternativas y las limitaciones están en [docs/concurrencia.md](docs/concurrencia.md).
- **JWT con HS256 y sin estado:** firmo los tokens con una clave simétrica que viene de una variable de entorno (la API no arranca si tiene menos de 32 bytes), porque una sola API firma y verifica; con varios servicios verificando usaría RS256. El precio de no guardar nada en el servidor es que un access token no se puede revocar antes de caducar, por eso dura poco.
- **Organización por funcionalidad:** el código está en paquetes `negocio/`, `servicio/` y `recurso/`, no separado por capas técnicas.
- **DTO como `record`:** los objetos de entrada y salida son inmutables y las entidades JPA quedan fuera de la API.

## Roadmap

| Etapa | Versión | Estado |
|---|---|---|
| 1. Base | v0.1.0 | ✅ Completada |
| 2. Reservas | v0.2.0 | ✅ Completada |
| 3. Usuarios y seguridad | v0.3.0 | 🟡 En curso |
| 4. Asincronía | v0.4.0 | ⬜ Pendiente |
| 5. Calidad y documentación | v0.5.0 | ⬜ Pendiente |
| 6. Observabilidad y despliegue | v0.6.0 | ⬜ Pendiente |
| 7. Frontend del cliente | v0.7.0 | ⬜ Pendiente |
| 8. Paneles de negocio y admin | v0.8.0 | ⬜ Pendiente |
| 9. Tiempo real y pulido | v1.0.0 | ⬜ Pendiente |
| 10. Kubernetes | v1.1.0 | ⬜ Pendiente |

## Licencia

MIT. Ver [LICENSE](LICENSE).
