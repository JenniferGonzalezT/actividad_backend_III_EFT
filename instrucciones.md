# 📋 Manual de Instrucciones y Despliegue - Banco XYZ

Este documento detalla los pasos necesarios para compilar, levantar, configurar y probar el ecosistema completo de microservicios de la **Evaluación Final Transversal (EFT)**.


## 🛠️ Prerrequisitos del Sistema
Asegúrate de contar con las siguientes herramientas instaladas en tu equipo:
* **Java 17** (compatible con JDK LTS).

* **Maven 3.9+** para la gestión de dependencias y empaquetado multi-módulo.

* **Docker y Docker Compose** para la orquestación de contenedores y bases de datos H2/Kafka.

* **cURL** y **Python 3** (para pruebas de comandos HTTP en terminal).


## 🚀 Paso a Paso para la Ejecución

### 1. Compilación del proyecto multi-módulo
Desde la carpeta raíz del proyecto (donde se encuentra el `pom.xml` principal), ejecuta el siguiente comando Maven para compilar todos los microservicios y generar sus respectivos archivos `.jar` (omitiendo las pruebas unitarias para agilizar el proceso):

```bash
mvn clean package -DskipTests
```
> *Verificación:* Deberás visualizar un `BUILD SUCCESS` en consola y los `.jar` generados en la carpeta `target` de cada microservicio.


### 2. Orquestación y levantamiento con Docker Compose
Una vez compilados los artefactos, ejecuta el siguiente comando para construir las imágenes y levantar el ecosistema completo en contenedores aislados:

```bash
docker compose up -d --build
```

#### Orden de arranque automático (Healthchecks):
1. `config-server` (Puerto 8888) y `discovery-server` / Eureka (Puerto 8761).

2. `auth-service` (Puerto 8081).

3. Microservicios de negocio (`cuentas-service`, `intereses-service`, `transacciones-service`) y el `bff-service` (Puerto 8085).


### 3. Verificación en el Servidor Eureka
Abre tu navegador web e ingresa a: http://localhost:8761

Deberás observar los 5 microservicios registrados y activos en estado UP:
- AUTH-SERVICE

- CUENTAS-SERVICE

- INTERESES-SERVICE

- TRANSACCIONES-SERVICE

- BFF-SERVICE


## 🧪 Pruebas de Funcionamiento y Endpoints
### A. Autenticación y Obtención de Token JWT
Genera un token de acceso mediante el servidor de autenticación:
```bash
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
```

### B. Pruebas del Patrón BFF (Backend for Frontend)
Utiliza el token obtenido para consultar los diferentes canales especializados:

1. Canal Web (Agregación de Cuentas e Intereses - ID 101):
```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8085/api/bff/web/cuenta/101
```

2. Canal App Móvil (Respuesta ligera optimizada):
```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8085/api/bff/movil/cuenta/101
```

3. Canal Cajero Automático / ATM (Consulta rápida de saldo):
```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8085/api/bff/cajero/cuenta/101
```


## 🛑 Detener el Ecosistema
Para apagar y limpiar los contenedores de Docker creados, ejecuta:
```bash
docker compose down
```
