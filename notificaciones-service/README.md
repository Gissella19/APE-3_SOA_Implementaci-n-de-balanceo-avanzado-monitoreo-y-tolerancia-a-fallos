# notificaciones-service

Servicio REST independiente para el taller **Construcción de un Servicio en SOA**.

## Importante

Este proyecto **NO se conecta con `venta-vehiculos-api`**.  
Solo crea y prueba el servicio de notificaciones solicitado en las Actividades 1 a 5.

## Requisitos

- Java 17
- Maven
- Postman

## Ejecutar

```bash
mvn spring-boot:run
```

El servicio queda disponible en:

```text
http://localhost:8081
```

## Endpoint

```http
POST http://localhost:8081/api/notificaciones
Content-Type: application/json
```

### Ejemplo correcto

```json
{
  "vehiculoId": 5,
  "destinatario": "usuario@correo.com",
  "tipo": "REGISTRO",
  "mensaje": "Vehículo registrado correctamente"
}
```

Respuesta esperada: **201 Created**

```json
{
  "success": true,
  "estado": "ENVIADA",
  "mensaje": "Notificación procesada correctamente"
}
```

## Validaciones

- `vehiculoId` debe estar presente y ser mayor que 0.
- `destinatario` no puede estar vacío.
- `mensaje` no puede estar vacío.
- `tipo` solo acepta `REGISTRO`, `ACTUALIZACION` o `ELIMINACION`.

Cuando falla una validación, la respuesta es **400 Bad Request**.
