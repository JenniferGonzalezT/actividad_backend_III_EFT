# Banco XYZ — Migración de Datos Legacy y Microservicios con Spring Cloud

Proyecto de modernización de procesos batch y arquitectura orientada a eventos de un banco ficticio (Banco XYZ). Esta versión (Semana 8) implementa **Spring Cloud Security con OAuth 2.0 Resource Server**, despliegue orquestado con **Docker Compose** y mecanismos de tolerancia a fallos mediante **Resilience4j** y **Apache Kafka**.


## Arquitectura Dockerizada

El ecosistema completo se levanta dentro de una red interna de Docker, donde los microservicios se descubren automáticamente mediante Eureka y validan sus tokens contra un esquema OAuth 2.0.

```text
 config-server (8888)        discovery-server / Eureka (8761)
         │                            │
         └──────────────┬─────────────┘
                        │  (config + registro)
        ┌───────────────┼────────────────┬─────────────────┐
        │               │                │                 │
  auth-service     transacciones-   intereses-service  cuentas-service
     (8081)        service (8082)      (8083)             (8084)
 (JWT Issuer)           │                │                 │
                        └──► intereses ──┘                 │
                                         └──► cuentas ─────┘
                                                           │
                        cuentas ──► transacciones ◄────────┘
```

- **config-server**: Spring Cloud Config Server (perfil `native`), sirve la configuración de todos los clientes desde `config-server/src/main/resources/config-repo/`.

- **discovery-server**: servidor Eureka standalone para Service Discovery.

- **auth-service**: emite y valida JWT (login con usuarios en memoria, roles `ADMIN`/`USER`).

- **transacciones-service**, **intereses-service**, **cuentas-service**: cada uno migra su CSV correspondiente con Spring Batch, expone una API REST protegida por JWT, y llama a otro servicio vía Feign con Resilience4j (Circuit Breaker + Retry + fallback), formando un triángulo de dependencias:
  - `transacciones-service` → `intereses-service`
  - `intereses-service` → `cuentas-service`
  - `cuentas-service` → `transacciones-service`

- **Docker Compose** (`docker-compose.yml`): Orquesta 6 microservicios Spring Boot, 1 broker Apache Kafka y 1 panel Kafka-UI, configurados con Healthchecks para asegurar un orden de arranque determinista (Config Server → Discovery Server → Microservicios).

- **OAuth 2.0 Resource Server**: Los microservicios de negocio ahora validan los roles del usuario implementando el estándar OAuth 2.0 de Spring Security, mediante un `JwtAuthenticationConverter` personalizado (`RolesClaimJwtAuthenticationConverter`).


### Módulos Maven

| Módulo | Puerto | Rol |
|---|---|---|
| `common-lib` | — | DTOs compartidos y converter de roles JWT (no es una app Spring Boot) |
| `config-server` | 8888 | Config Server centralizado |
| `discovery-server` | 8761 | Eureka |
| `auth-service` | 8081 | Emisión/validación de JWT |
| `transacciones-service` | 8082 | Migración + API de `transacciones.csv` |
| `intereses-service` | 8083 | Migración + API de `intereses.csv` |
| `cuentas-service` | 8084 | Migración + API de `cuentas_anuales.csv` |


### Datos de origen

Cada microservicio de negocio migra su CSV desde `data/semana_3/` (copiado a `src/main/resources/data/` de cada módulo para no depender de rutas externas) hacia una base H2 en memoria propia (patrón *database per service*), usando un job de Spring Batch con políticas de skip para los problemas típicos de datos legacy:

1. **`transacciones.csv`**: fechas en 4 formatos (`yyyy-MM-dd`, `yyyy/MM/dd`, `dd-MM-yyyy`, `dd/MM/yyyy`), montos vacíos/negativos/cero, `tipo` inválido (`invalid`, `desconocido`).

2. **`intereses.csv`**: saldos vacíos/≤0, edades fuera de rango (1–100), `tipo` inválido (`-1`, `unknown`), filas duplicadas.

3. **`cuentas_anuales.csv`**: mismas 4 variantes de fecha, montos vacíos/negativos, `transaccion` con variante acentuada (`depósito` se normaliza, no se descarta; `pago` es inválido), `descripcion` vacía se reemplaza por `"Sin descripción"` en vez de descartar la fila.

Las filas inválidas se omiten (skip) vía Spring Batch (`faultTolerant().skip(...)`) y quedan registradas en el log y en el contador de migración de cada servicio (`GET /api/migracion/status`).


## Arquitectura de eventos (Kafka)

Las escrituras entre servicios usan una **saga coreografiada sobre Kafka**: `POST /api/transacciones` (transacciones-service) → `transaccion.registrada` → cuentas-service → `cuenta.movimiento-aplicado` → intereses-service → `interes.recalculado` → transacción `CONFIRMADA`. Los fallos usan reintentos, Dead Letter Topics y compensación (`transaccion.fallida`). Diagrama, tópicos y pruebas en [docs/arquitectura-eventos.md](docs/arquitectura-eventos.md). Requiere `docker compose up -d` (Kafka en `localhost:9092`).


## Requisitos

- Docker y Docker Compose.
- Java 17
- Maven 3.9+
- Puertos libres: `8761`, `8888`, `8081`, `8082`, `8083`, `8084`, `9092`, `8090`.


## Cómo levantar el proyecto en Docker

### 1. Compilar los artefactos Java
Desde la raíz del proyecto, ejecuta Maven para generar los `.jar` que Docker utilizará:

```bash
mvn clean package -DskipTests
```

### 2. Orquestar los contenedores con Docker Compose
Lanza toda la infraestructura con el siguiente comando:

```bash
docker compose up -d --build
```

> **Nota de arranque**: Docker esperará a que `config-server` y `discovery-server` estén saludables (healthchecks) antes de levantar los microservicios. Este proceso puede tomar entre 1 y 2 minutos.


### 3. Verificar el estado
Abre en tu navegador http://localhost:8761. Deberás ver los 4 microservicios (`AUTH-SERVICE`, `CUENTAS-SERVICE`, `INTERESES-SERVICE`y `TRANSACCIONES-SERVICE`) registrados en estado `UP`.


## Pruebas de Seguridad (OAuth 2.0)

Usuarios de demostración (en memoria en `auth-service`):

| Usuario | Password | Roles |
|---|---|---|
| `admin` | `admin123` | `ADMIN`, `USER` |
| `user` | `user123` | `USER` |

```bash
# Login: Obtener Token JWT (auth-service)
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# Prueba de acceso Autorizado (Código 200 OK)
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/transacciones

# Prueba de acceso Denegado (Código 401 Unauthorized)
curl -i http://localhost:8082/api/transacciones

# Endpoint solo ADMIN con usuario USER -> 403
curl -i -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/migracion/rerun
```


## Referencia de endpoints

### auth-service (8081)

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| POST | `/auth/login` | pública | Devuelve `{token, tokenType, expiresInMs}` |
| GET | `/auth/me` | JWT | Usuario y roles del token actual |

### transacciones-service (8082) / intereses-service (8083) / cuentas-service (8084)

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| GET | `/api/<recurso>` | JWT | Listado paginado de los datos migrados |
| GET | `/api/<recurso>/{id}` | JWT | Registro por id interno |
| GET | `/api/migracion/status` | JWT | Contadores `leidos/escritos/omitidos` |
| POST | `/api/migracion/rerun` | JWT + `ADMIN` | Re-ejecuta el job de migración |

`<recurso>` es `transacciones`, `intereses` o `cuentas` según el servicio.

### Endpoints adicionales de resumen y llamada cross-service:

| Servicio | Ruta | Descripción |
|---|---|---|
| transacciones-service | `GET /api/transacciones/resumen-con-intereses/{cuentaId}` | Llama a `intereses-service` (Circuit Breaker + Retry + fallback) |
| intereses-service | `GET /api/intereses/resumen/{cuentaId}` | Resumen propio (consumido por transacciones-service) |
| intereses-service | `GET /api/intereses/resumen-con-cuenta/{cuentaId}` | Llama a `cuentas-service` (Circuit Breaker + Retry + fallback) |
| cuentas-service | `GET /api/cuentas/resumen/{cuentaId}` | Resumen propio agregado (consumido por intereses-service) |
| cuentas-service | `GET /api/cuentas/resumen-con-transaccion/{transaccionId}` | Llama a `transacciones-service` (Circuit Breaker + Retry + fallback) |


## Stack técnico

- Java 17, Maven multi-módulo
- Spring Boot 3.3.13 / Spring Cloud 2023.0.6 ("Leyton")
- Spring Cloud Config, Netflix Eureka, OpenFeign, LoadBalancer
- Resilience4j 2.4.0 (Circuit Breaker, Retry)
- Spring Security (OAuth2 Resource Server, JWT HS256 vía `jjwt` 0.13.0)
- Spring Batch + Spring Data JPA + H2 (una base en memoria por servicio)

## Datos legacy originales

Los archivos en `data/` representan los distintos tipos de datos legacy usados en los procesos batch, organizados por semana (`semana_1`, `semana_2`, `semana_3`). Los microservicios usan `data/semana_3` (el dataset más completo) como fuente.
