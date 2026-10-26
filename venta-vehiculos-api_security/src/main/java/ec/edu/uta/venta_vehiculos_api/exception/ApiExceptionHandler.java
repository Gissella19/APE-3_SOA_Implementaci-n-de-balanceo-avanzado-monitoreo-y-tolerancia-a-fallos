package ec.edu.uta.venta_vehiculos_api.exception;

import java.net.URI;

import java.util.UUID;

import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.http.*;

import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(Exception.class)

    public ResponseEntity<ProblemDetail> manejar(
            Exception ex, HttpServletRequest req) {

        int status = 500;

        String codigo = "ERROR_INTERNO";

        String detalle = "No fue posible completar la solicitud";

        if (ex instanceof ApiError e) {

            status = e.status;
            codigo = e.codigo;
            detalle = e.getMessage();

        } else if (ex instanceof MethodArgumentNotValidException e) {

            status = 422;
            codigo = "VALIDACION";

            detalle = e.getBindingResult().getFieldErrors().stream()

                .map(x -> x.getField() + ": " + x.getDefaultMessage())

                .reduce((a,b) -> a + "; " + b)

                .orElse("Datos inválidos");

        } else if (ex instanceof HttpMessageNotReadableException) {

            status = 400;
            codigo = "JSON_INVALIDO";
            detalle = "JSON ilegible o tipo de dato incorrecto";

        } else if (ex instanceof DataIntegrityViolationException) {

            status = 409;
            codigo = "CONFLICTO_DATOS";
            detalle = "Duplicado o relación que impide la operación";

        } else if (ex instanceof IllegalArgumentException) {

            status = 400;
            codigo = "SOLICITUD_INVALIDA";
            detalle = ex.getMessage();

        } else if (ex instanceof ErrorResponse e) {

            status = e.getStatusCode().value();

            codigo = "ERROR_HTTP";

            detalle = e.getBody().getDetail();
        }

        String trace = UUID.randomUUID().toString();

        if (status >= 500)
            LoggerFactory.getLogger(getClass())
                .error("Error " + trace, ex);

        ProblemDetail p = ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(status), detalle);

        p.setType(URI.create("urn:vehiculos:" + codigo.toLowerCase()));

        p.setInstance(URI.create(req.getRequestURI()));

        p.setProperty("codigo", codigo);

        p.setProperty("traceId", trace);

        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(p);

    }

}