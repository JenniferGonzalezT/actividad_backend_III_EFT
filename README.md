# 🏦 Banco XYZ: Modernización de Sistema Legacy a Arquitectura de Microservicios Cloud, Spring Batch y Patrón BFF


## 🚀 Descripción General del Proyecto
Este proyecto representa la solución integral para la modernización de la plataforma tecnológica del **Banco XYZ**, migrando su infraestructura *legacy* (basada en mainframes, COBOL y scripts shell) hacia una arquitectura moderna de **microservicios distribuidos, resilientes y orientados a eventos** alojados en la nube.

La solución implementa los pilares avanzados de la ingeniería de software backend:
1. **Migración de Procesos Batch:** Digitalización de reportes masivos (cuentas anuales, intereses mensuales y transacciones diarias) con Spring Batch, aplicando tolerancia a fallos, omisiones (*skip policies*) y **procesamiento paralelo de alto rendimiento mediante hilos (`TaskExecutor`)**.

2. **Patrón Backend for Frontend (BFF):** Implementación de una capa especializada con endpoints personalizados para optimizar la entrega de datos y la seguridad en tres canales distintos: **Portal Web (con agregación de servicios)**, **App Móvil (respuestas ligeras)** y **Cajero Automático / ATM**.

3. **Seguridad Distribuida:** Autenticación y autorización centralizada mediante **OAuth 2.0 y tokens JWT**, protegiendo los recursos a través de un servidor de autenticación dedicado (`auth-service`) y propagación segura de credenciales.

4. **Tolerancia a Fallos y Resiliencia:** Incorporación de **Resilience4j (Circuit Breakers y Fallbacks)** en los clientes OpenFeign para evitar la propagación de errores ante caídas de servicios.

5. **Arquitectura Orientada a Eventos:** Integración de **Apache Kafka** para gestionar transacciones distribuidas de forma asíncrona (Patrón Saga coreografiado).

6. **Orquestación en Contenedores:** Despliegue estandarizado de todo el ecosistema mediante **Docker y Docker Compose** con controles de salud (*healthchecks*).


## 🛠️ Stack Tecnológico
* **Core:** Java 17, Spring Boot 3.3.13, Maven (Multi-módulo)

* **Infraestructura Cloud:** Spring Cloud Config Server, Netflix Eureka (Discovery Server)

* **Seguridad:** Spring Security, OAuth 2.0 Resource Server, JSON Web Tokens (JWT)

* **Procesamiento Masivo:** Spring Batch, ThreadPoolTaskExecutor (Multithreading)

* **Mensajería Asíncrona:** Apache Kafka, Kafka UI

* **Resiliencia:** Resilience4j (Circuit Breaker, Fallbacks)

* **Contenedorización:** Docker, Docker Compose


## 📂 Estructura de Módulos Maven
| Módulo | Puerto | Descripción Funcional |
|---|---|---|
| `common-lib` | — | Librería transversal que agrupa DTOs compartidos (`record`), eventos de Kafka y conversores de roles JWT. |
| `config-server` | 8888 | Servidor de configuración centralizada (perfil *native*) para todos los microservicios. |
| `discovery-server` | 8761 | Servidor de registro y descubrimiento de servicios (Netflix Eureka). |
| `auth-service` | 8081 | Servidor de Autenticación que emite y valida tokens JWT (usuarios en memoria con roles `ADMIN`/`USER`). |
| `cuentas-service` | 8084 | Gestión de cuentas, migración Batch paralelizada de `cuentas_anuales.csv` y base de datos H2 independiente. |
| `intereses-service` | 8083 | Gestión de intereses, migración Batch paralelizada de `intereses.csv` y base de datos H2 independiente. |
| `transacciones-service` | 8082 | Procesamiento de pagos/transacciones, migración Batch paralelizada de `transacciones.csv` y base H2. |
| `bff-service` | 8085 | Capa **Backend for Frontend** con canales dedicados: Web (agregación cross-service de cuentas, intereses y transacciones), Móvil y Cajero Automático (ATM). |


## ⚙️ Arquitectura Dockerizada y Arranque

El ecosistema opera dentro de una red aislada de Docker. Los microservicios descubren sus dependencias de forma dinámica a través de Eureka y obtienen sus propiedades del Config Server.

```text
 config-server (8888)        discovery-server / Eureka (8761)
         |                            |
         └──────────────┬─────────────┘
                        │  (config + registro)
        ┌───────────────┼────────────────┬─────────────────┬────────────────┐
        │               │                │                 │                │
  auth-service     cuentas-service  intereses-service  transacciones-    bff-service
     (8081)            (8084)           (8083)         service (8082)      (8085)
 (JWT Issuer)          │                │                 │          (Web/Movil/ATM)
                       └─────────────── Kafka ────────────┘
```


## 💾 Datos de origen

Cada microservicio de negocio migra su CSV desde `data/semana_3/` (copiado a `src/main/resources/data/` de cada módulo para no depender de rutas externas) hacia una base H2 en memoria propia (patrón *database per service*), usando un job de Spring Batch con políticas de skip para los problemas típicos de datos legacy:

1. **`transacciones.csv`**: fechas en 4 formatos (`yyyy-MM-dd`, `yyyy/MM/dd`, `dd-MM-yyyy`, `dd/MM/yyyy`), montos vacíos/negativos/cero, `tipo` inválido (`invalid`, `desconocido`).

2. **`intereses.csv`**: saldos vacíos/≤0, edades fuera de rango (1–100), `tipo` inválido (`-1`, `unknown`), filas duplicadas.

3. **`cuentas_anuales.csv`**: mismas 4 variantes de fecha, montos vacíos/negativos, `transaccion` con variante acentuada (`depósito` se normaliza, no se descarta; `pago` es inválido), `descripcion` vacía se reemplaza por `"Sin descripción"` en vez de descartar la fila.

Las filas inválidas se omiten (skip) vía Spring Batch (`faultTolerant().skip(...)`) y quedan registradas en el log y en el contador de migración de cada servicio (`GET /api/migracion/status`).


## Instrucciones para compilar y ejecutar:

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
Abre en tu navegador http://localhost:8761. Deberás ver los 5 microservicios (`AUTH-SERVICE`, `BFF-SERVICE`, `CUENTAS-SERVICE`, `INTERESES-SERVICE`y `TRANSACCIONES-SERVICE`) registrados en estado `UP`.


## Pruebas de Seguridad (OAuth 2.0) y Endpoints del BFF

Usuarios de demostración (en memoria en `auth-service`):

| Usuario | Password | Roles |
|---|---|---|
| `admin` | `admin123` | `ADMIN`, `USER` |
| `user` | `user123` | `USER` |

### Ejemplo de uso del Patrón BFF (Canales):
```bash
# 1. Obtener Token JWT desde auth-service
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 2. Consultar canal Web (Datos agregados de Cuentas, Intereses y Transacciones)
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8085/api/bff/web/cuenta/101

# 3. Consultar canal Móvil (Respuesta ligera y optimizada)
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8085/api/bff/movil/cuenta/101

# 4. Consultar canal Cajero Automático / ATM (Operación rápida)
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8085/api/bff/cajero/cuenta/101
```
