package ec.edu.uta.monitor.dto;

public record EstadisticasResponse(
        long totalPeticiones,
        long respuestas2xx,
        long respuestas4xx,
        long respuestas5xx,
        double promedioRespuestaMs,
        String criterioVentana) { }