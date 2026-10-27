package ec.edu.uta.monitor.dto;

import java.time.OffsetDateTime;

public record RegistroLog(
        OffsetDateTime timestamp,
        String metodo,
        String uri,
        int status,
        String upstreamAddr,
        double requestTimeSeg) { }