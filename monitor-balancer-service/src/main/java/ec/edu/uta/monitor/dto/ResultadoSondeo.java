package ec.edu.uta.monitor.dto;

import java.time.OffsetDateTime;

public record ResultadoSondeo(
        String destino,
        int puerto,
        String url,
        EstadoComponente estado,
        Long latenciaMs,
        Integer httpStatus,
        OffsetDateTime revision) { }