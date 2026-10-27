package ec.edu.uta.monitor.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record EstadoResponse(
        String nginx,
        EstadoGeneral estadoGeneral,
        List<InstanciaResumen> instancias,
        OffsetDateTime ultimaRevision) { }