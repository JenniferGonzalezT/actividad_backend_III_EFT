# Banco XYZ — Migración de Datos Legacy y Microservicios con Spring Cloud

Proyecto de migración y modernización de procesos batch de un banco ficticio (Banco XYZ). Parte de una arquitectura de microservicios con Spring Cloud que migra los datos legacy (CSV con errores típicos de un sistema batch antiguo) hacia bases de datos propias de cada servicio, y los expone mediante APIs REST protegidas con JWT, con Service Discovery, Config Server centralizado y tolerancia a fallos entre servicios.

## Arquitectura

```
config-server (8888)        discovery-server / Eureka (8761)
        │                            │
        └──────────────┬─────────────┘
                        │  (config + registro)
        ┌───────────────┼────────────────┬─────────────────┐
        │               │                │                 │
  auth-service     transacciones-   intereses-service  cuentas-service
     (8081)        service (8082)      (8083)             (8084)
                        │                │                 │
                        └──► intereses ──┘                 │
                                         └──► cuentas ──────┘
                                                             │
                        cuentas ──► transacciones ◄─────────┘
```

- **config-server**: Spring Cloud Config Server (perfil `native`), sirve la configuración de todos los clientes desde `config-server/src/main/resources/config-repo/`.
- **discovery-server**: servidor Eureka standalone para Service Discovery.
- **auth-service**: emite y valida JWT (login con usuarios en memoria, roles `ADMIN`/`USER`).
- **transacciones-service**, **intereses-service**, **cuentas-service**: cada uno migra su CSV correspondiente con Spring Batch, expone una API REST protegida por JWT, y llama a otro servicio vía Feign con Resilience4j (Circuit Breaker + Retry + fallback), formando un triángulo de dependencias:
  - `transacciones-service` → `intereses-service`
  - `intereses-service` → `cuentas-service`
  - `cuentas-service` → `transacciones-service`

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

- Java 17
- Maven 3.9+
- Puertos libres: 8761, 8888, 8081, 8082, 8083, 8084

## Cómo levantar el proyecto

### 1. Compilar todo

Desde la raíz del proyecto (`Formativa Backend/`):

```bash
mvn clean package -DskipTests
```

### 2. Levantar los servicios en orden

El **config-server** y el **discovery-server** deben estar arriba antes que el resto (los demás dependen de config al arrancar, `spring.cloud.config.fail-fast=true` con reintentos).

```bash
# Terminal 1
java -jar discovery-server/target/discovery-server-1.0.0.jar

# Terminal 2
java -jar config-server/target/config-server-1.0.0.jar
```

Esperar a que ambos respondan `UP`:

```bash
curl http://localhost:8761/actuator/health
curl http://localhost:8888/actuator/health
```

Luego, en cualquier orden:

```bash
# Terminal 3
java -jar auth-service/target/auth-service-1.0.0.jar

# Terminal 4
java -jar transacciones-service/target/transacciones-service-1.0.0.jar

# Terminal 5
java -jar intereses-service/target/intereses-service-1.0.0.jar

# Terminal 6
java -jar cuentas-service/target/cuentas-service-1.0.0.jar
```

También se puede usar `mvn -pl <modulo> spring-boot:run` en vez de `java -jar`.

Cada servicio ejecuta su migración de datos automáticamente al arrancar (job de Spring Batch al inicio de la aplicación).

## Verificación

### Config Server centralizado

```bash
curl http://localhost:8888/transacciones-service/default
```

Debe devolver la configuración combinada (puerto, datasource, Eureka, Resilience4j, etc.).

### Service Discovery

Abrir `http://localhost:8761` en el navegador: deben listarse `AUTH-SERVICE`, `TRANSACCIONES-SERVICE`, `INTERESES-SERVICE` y `CUENTAS-SERVICE`, todos `UP`.

### Autenticación y autorización

Usuarios de demostración (en memoria en `auth-service`):

| Usuario | Password | Roles |
|---|---|---|
| `admin` | `admin123` | `ADMIN`, `USER` |
| `user` | `user123` | `USER` |

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# Endpoint protegido
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/transacciones

# Sin token -> 401
curl -i http://localhost:8082/api/transacciones

# Endpoint solo ADMIN con usuario USER -> 403
curl -i -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/migracion/rerun
```

### Estado de la migración

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/migracion/status
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/migracion/status
curl -H "Authorization: Bearer $TOKEN" http://localhost:8084/api/migracion/status
```

Devuelve `{"leidos":N,"escritos":N,"omitidos":N}` por servicio.

### Tolerancia a fallos (Circuit Breaker + Retry + Fallback)

Con todos los servicios arriba:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/transacciones/resumen-con-intereses/133
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/intereses/resumen-con-cuenta/103
curl -H "Authorization: Bearer $TOKEN" http://localhost:8084/api/cuentas/resumen-con-transaccion/1
```

Cada respuesta debe traer `"fallback":false` (llamada real al servicio downstream vía Feign + Eureka).

Para forzar el fallback, detener un servicio downstream (por ejemplo `intereses-service`) y repetir la llamada en `transacciones-service`:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/transacciones/resumen-con-intereses/133
# -> {"fallback":true, ...}

curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/actuator/circuitbreakers
# -> el estado de "interesesClient" cambia de CLOSED a OPEN/HALF_OPEN tras varios fallos
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

Endpoints adicionales de resumen y llamada cross-service:

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

### Problemas simulados en los datos

- **Montos negativos o cero**: anomalías que requieren validación.
- **Formatos de fecha inconsistentes**: mezcla de `yyyy-MM-dd`, `yyyy/MM/dd`, `dd-MM-yyyy`, `dd/MM/yyyy`, incluyendo fechas inválidas (ej. `2024-13-01`).
- **Datos faltantes o nulos**: campos como saldo o descripción vacíos.
- **Registros duplicados**: múltiples filas con los mismos datos.
- **Valores fuera de rango**: edades no realistas o tipos de transacción/cuenta no válidos.

Estos problemas se gestionan mediante políticas de skip de Spring Batch en cada microservicio para garantizar la integridad y calidad de los datos migrados.
