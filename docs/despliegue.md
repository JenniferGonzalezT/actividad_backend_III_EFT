# 🚢 Guía de Despliegue y Arquitectura Cloud - Banco XYZ

Este documento detalla la estrategia de contenedorización, orquestación de red y despliegue en entornos productivos mediante **Docker y Docker Compose** para el ecosistema de microservicios del Banco XYZ.


## 🏗️ Estrategia de Contenerización
Cada microservicio del sistema (`config-server`, `discovery-server`, `auth-service`, `cuentas-service`, `intereses-service`, `transacciones-service` y `bff-service`) cuenta con su propio **`Dockerfile`** optimizado basado en una imagen de Java (OpenJDK 17), empaquetando de forma aislada el artefacto ejecutable (`.jar`) generado por Maven.

### Principios aplicados:
* **Database per Service:** Cada microservicio de negocio opera con su propia instancia de base de datos H2 en memoria, asegurando un acoplamiento débil e independencia operativa.

* **Configuración Centralizada:** Los servicios obtienen sus propiedades de entorno de forma remota a través del `config-server` al iniciar.

* **Service Discovery:** El registro y descubrimiento dinámico de direcciones IP/puertos se gestiona automáticamente mediante Netflix Eureka.


## 🐳 Orquestación con Docker Compose
El archivo `docker-compose.yml` en la raíz del proyecto gestiona la ejecución coordinada de todo el ecosistema dentro de una **red interna de Docker**, exponiendo únicamente los puertos necesarios hacia el host local.

### Topología de Contenedores y Puertos

| Contenedor / Servicio | Puerto Interno | Puerto Externo (Host) | Rol Principal |
|---|---|---|---|
| `config-server` | 8888 | 8888 | Servidor de configuración centralizada |
| `discovery-server` | 8761 | 8761 | Servidor Eureka (Service Discovery) |
| `auth-service` | 8081 | 8081 | Servidor de Autenticación (Emisor OAuth2 / JWT) |
| `cuentas-service` | 8084 | 8084 | Gestión de cuentas y Spring Batch paralelizado |
| `intereses-service` | 8083 | 8083 | Gestión de intereses y Spring Batch paralelizado |
| `transacciones-service` | 8082 | 8082 | Procesamiento de pagos y Spring Batch paralelizado |
| `bff-service` | 8085 | 8085 | Backend for Frontend (Canales Web, Móvil y ATM) |
| `kafka` | 9092 | 9092 | Broker de mensajería asíncrona (Arquitectura orientada a eventos) |


## 🔄 Arranque Determinista y Healthchecks
Para evitar fallas de conexión iniciales (por ejemplo, que un microservicio intente registrarse en Eureka antes de que este se encuentre completamente operativo), el sistema implementa **controles de salud (*healthchecks*)** y dependencias estrictas (`service_healthy`) en el archivo `docker-compose.yml`:

1. **Fase 1:** Se despliegan el `config-server` y el `discovery-server`. El sistema espera a que respondan satisfactoriamente a sus rutas de salud.

2. **Fase 2:** Se despliega el `auth-service` junto con el broker de mensajes Kafka.

3. **Fase 3:** Se despliegan los microservicios de negocio (`cuentas`, `intereses`, `transacciones`) y finalmente la capa de acceso del cliente (`bff-service`).


## 🛡️ Consideraciones de Seguridad y Producción

* **Gestión de Secretos:** Las claves de firma para los tokens JWT (`app.jwt.secret`) están parametrizadas en las propiedades externas del Config Server, permitiendo su inyección mediante variables de entorno seguras en entornos productivos.

* **Comunicaciones sin Estado (Stateless):** Los microservicios y el BFF operan bajo una política de sesiones sin estado (`SessionCreationPolicy.STATELESS`), validando la integridad y vigencia del token JWT en cada cabecera `Authorization: Bearer`.

* **Tolerancia a Fallos:** Los clientes OpenFeign incorporan mecanismos de resiliencia mediante **Resilience4j (Circuit Breakers y Fallbacks)**, previniendo la saturación en cascada ante caídas temporales de servicios downstream.
