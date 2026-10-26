package ec.edu.uta.notificaciones.exception;

import ec.edu.uta.notificaciones.dto.NotificacionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<NotificacionResponse> manejarValidacion(
            IllegalArgumentException ex) {

        NotificacionResponse response =
                new NotificacionResponse(
                        false,
                        null,
                        ex.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
