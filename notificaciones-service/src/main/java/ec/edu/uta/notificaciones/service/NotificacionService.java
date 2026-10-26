package ec.edu.uta.notificaciones.service;

import ec.edu.uta.notificaciones.dto.NotificacionRequest;
import ec.edu.uta.notificaciones.dto.NotificacionResponse;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class NotificacionService {

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of("REGISTRO", "ACTUALIZACION", "ELIMINACION");

    public NotificacionResponse procesar(NotificacionRequest request) {

        if (request.getVehiculoId() == null || request.getVehiculoId() <= 0) {
            throw new IllegalArgumentException("vehiculoId debe estar presente y ser válido");
        }

        if (request.getDestinatario() == null ||
                request.getDestinatario().trim().isEmpty()) {
            throw new IllegalArgumentException("El destinatario no puede estar vacío");
        }

        if (request.getMensaje() == null ||
                request.getMensaje().trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        if (request.getTipo() == null ||
                !TIPOS_PERMITIDOS.contains(request.getTipo().toUpperCase())) {
            throw new IllegalArgumentException("Tipo de notificación no válido");
        }


        System.out.println(
        "================================="
        );

        System.out.println(
                "NOTIFICACIÓN RECIBIDA"
        );

        System.out.println(
                "Vehículo: "
                        + request.getVehiculoId()
        );

        System.out.println(
                "Tipo: "
                        + request.getTipo()
        );

        System.out.println(
                "Mensaje: "
                        + request.getMensaje()
        );

        System.out.println(
                "================================="
        );
        // En este taller no se envía realmente correo/SMS.
        // Solo se simula que la notificación fue procesada.
        return new NotificacionResponse(
                true,
                "ENVIADA",
                "Notificación procesada correctamente"
        );
    }
}
