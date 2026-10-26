# Guía Práctica APE 03: Implementación de Balanceo Avanzado, Monitoreo y Tolerancia a Fallos de una Web API REST con NGINX y Spring Boot

**Universidad Técnica de Ambato (UTA)**  
**Facultad de Ingeniería en Sistemas, Electrónica e Industrial**  
**Carrera de Tecnologías de la Información / Software — Arquitectura Orientada a Servicios (SOA)**

---

## 1. Descripción de la Solución

Esta solución implementa una infraestructura distribuida de alta disponibilidad para la gestión de vehículos basada en los principios de arquitectura orientada a servicios (SOA). Se compone de réplicas sin estado (*stateless*) protegidas por JSON Web Tokens (JWT), un proxy inverso y balanceador de carga ponderado 3:1 con NGINX (puerto 8090) con tolerancia a fallos pasiva, y un servicio desacoplado de notificaciones (puerto 8081).

---

## 2. Arquitectura y Puertos de Red

| Componente | Dirección / Puerto | Responsabilidad Arquitectónica |
| :--- | :--- | :--- |
| **Cliente REST / Postman** | Conexión HTTP hacia `:8090` | Cliente consumidor; obtiene token JWT y consume la API a través del balanceador. |
| **NGINX (Proxy Inverso)** | `127.0.0.1:8090` | Punto de entrada unificado, balanceo ponderado 3:1, reintentos (`proxy_next_upstream`), endpoint `/nginx-ping` y registro en `access.log`. |
| **Vehículos Instancia A** | `127.0.0.1:8080` | Réplica principal del microservicio de vehículos (peso = 3 en NGINX). Expone `/actuator/health` y `/api/vehiculos/instancia`. |
| **Vehículos Instancia B** | `127.0.0.1:8082` | Réplica secundaria concurrente (peso = 1 en NGINX). Comparte la misma clave JWT y base de datos relacional. |
| **notificaciones-service** | `127.0.0.1:8081` | Microservicio desacoplado de recepción y registro de avisos de eventos vehiculares. |
| **MySQL Database** | `127.0.0.1:3306` | Persistencia relacional compartida (`venta_vehiculos`). |
| **monitor-balancer-service** | `127.0.0.1:8083` | *(Módulo de observabilidad activo, sondeo periódico y cálculo de métricas en desarrollo).* |

---

## 3. Requisitos del Sistema

* **Java JDK:** Versión 17 o superior.
* **Apache Maven:** 3.8+ (o uso del wrapper integrado `mvnw` / `mvnw.cmd`).
* **Base de Datos:** MySQL / MariaDB (ej. XAMPP) en el puerto 3306 con la base `venta_vehiculos`.
* **Servidor Proxy:** NGINX instalado en el host local.

---

## 4. Orden de Arranque Estricto

Para garantizar la correcta resolución de dependencias de red, inicie los componentes en el siguiente orden secuencial:

### Paso 1: Base de Datos MySQL (Puerto 3306)
Inicie el servicio de MySQL desde el **XAMPP Control Panel** (verifique que el puerto 3306 esté en estado activo).

### Paso 2: Servicio de Notificaciones (Puerto 8081)
Abra una terminal en la raíz del proyecto y ejecute:
```powershell
cd notificaciones-service
.\mvnw.cmd spring-boot:run
```
*Verifique en consola el mensaje: `Started NotificacionesServiceApplication on port 8081`.*

### Paso 3: Vehículos Instancia A (Puerto 8080 - Primaria)
Abra una segunda terminal y ejecute:
```powershell
cd venta-vehiculos-api_security
.\mvnw.cmd spring-boot:run
```
*Verifique en consola el mensaje: `Started VentaVehiculosApiApplication on port 8080`.*

### Paso 4: Vehículos Instancia B (Puerto 8082 - Secundaria)
Abra una tercera terminal y ejecute la réplica sobreescribiendo el puerto dinámico y el identificador:
```powershell
cd venta-vehiculos-api_security
$env:SERVER_PORT="8082"; $env:APP_INSTANCE_ID="Vehiculos-B"
.\mvnw.cmd spring-boot:run
```
*(Alternativa en una sola línea: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082 --app.instancia.id=Vehiculos-B"`).*  
*Verifique en consola el mensaje: `Started VentaVehiculosApiApplication on port 8082`.*

### Paso 5: Servidor Proxy NGINX (Puerto 8090)
Copie el archivo [`nginx/nginx.conf`](nginx/nginx.conf) en el directorio de configuración de su NGINX local (`C:\nginx\conf\nginx.conf`) y ejecute:
```powershell
cd C:\nginx
.\nginx.exe -t       # Comprobación de sintaxis
.\nginx.exe          # O '.\nginx.exe -s reload' si ya está ejecutándose
```

---

## 5. Catálogo de Endpoints Verificados

### 1. Obtención de Token JWT
* **Método:** `POST http://127.0.0.1:8080/oauth/token` (o por NGINX `http://127.0.0.1:8090/oauth/token`)
* **Autenticación:** Basic Auth
  * **Usuario:** `vehiculos-client`
  * **Contraseña:** `cliente-local-aula-2026-cambiar`
* **Tipo de Contenido:** `application/x-www-form-urlencoded`
* **Campos:**
  * `grant_type`: `password`
  * `username`: `admin-local`
  * `password`: `cambiar-contrasena-aula-2026`
* **Respuesta Exitosa:** `200 OK` con `access_token` JWT.

### 2. Identificación de Instancia (Stateless)
* **Método:** `GET http://127.0.0.1:8080/api/vehiculos/instancia` (Instancia A)
* **Método:** `GET http://127.0.0.1:8082/api/vehiculos/instancia` (Instancia B)
* **Método:** `GET http://127.0.0.1:8090/api/vehiculos/instancia` (Balanceado por NGINX)
* **Cabecera requerida:** `Authorization: Bearer <access_token>`
* **Respuestas esperadas:**
  * Instancia A: `{"instancia": "instancia-8080", "puerto": 8080}`
  * Instancia B: `{"instancia": "Vehiculos-B", "puerto": 8082}`

### 3. Sonda de Salud de Infraestructura (Actuator)
* **Método:** `GET http://127.0.0.1:8080/actuator/health` y `GET http://127.0.0.1:8082/actuator/health`
* **Acceso:** Público local (no requiere JWT).
* **Respuesta:** `{"status": "UP"}`.

### 4. Diagnóstico Local de NGINX
* **Método:** `GET http://127.0.0.1:8090/nginx-ping`
* **Acceso:** Exclusivo localhost (`127.0.0.1`). Excluido del `access.log`.
* **Respuesta:** `{"status": "UP", "mensaje": "NGINX operativo"}`.

---

## 6. Estructura del Repositorio

```text
├── .gitignore                      # Exclusión de compilados, binarios y temporales
├── README.md                       # Documentación técnica de arquitectura y despliegue
├── nginx/
│   └── nginx.conf                  # Configuración validada de NGINX con upstream 3:1
├── notificaciones-service/         # Microservicio de eventos y avisos en puerto 8081
└── venta-vehiculos-api_security/   # Microservicio de catálogo y seguridad en puertos 8080 y 8082
```
