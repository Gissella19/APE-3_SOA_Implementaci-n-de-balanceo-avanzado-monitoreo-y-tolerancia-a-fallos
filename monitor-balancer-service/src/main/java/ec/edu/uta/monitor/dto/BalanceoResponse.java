package ec.edu.uta.monitor.dto;

import java.time.OffsetDateTime;

public record BalanceoResponse(
        OffsetDateTime inicioVentana,
        OffsetDateTime finVentana,
        long totalSolicitudes,
        long instanciaA,
        long instanciaB,
        long sinUpstream,
        double porcentajeA,
        double porcentajeB,
        double porcentajeSinUpstream) { }