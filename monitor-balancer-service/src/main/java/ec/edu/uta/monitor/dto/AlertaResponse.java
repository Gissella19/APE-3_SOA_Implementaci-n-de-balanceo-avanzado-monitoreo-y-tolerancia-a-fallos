package ec.edu.uta.monitor.dto;

import java.time.OffsetDateTime;

public record AlertaResponse(
        OffsetDateTime timestamp,
        String componente,
        String anterior,
        String nuevo,
        String mensaje) { }