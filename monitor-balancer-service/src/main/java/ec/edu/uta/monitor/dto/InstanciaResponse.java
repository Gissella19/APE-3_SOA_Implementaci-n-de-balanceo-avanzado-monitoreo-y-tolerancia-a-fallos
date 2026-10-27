package ec.edu.uta.monitor.dto;

import java.time.OffsetDateTime;

public record InstanciaResponse(
        int puerto,
        String urlConsultada,
        EstadoComponente estado,
        Long latenciaMs,
        OffsetDateTime ultimaRevision) { }