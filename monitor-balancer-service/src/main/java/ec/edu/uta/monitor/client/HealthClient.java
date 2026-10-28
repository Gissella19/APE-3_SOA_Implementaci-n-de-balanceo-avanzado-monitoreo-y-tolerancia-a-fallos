package ec.edu.uta.monitor.client;

import ec.edu.uta.monitor.dto.EstadoComponente;
import ec.edu.uta.monitor.dto.ResultadoSondeo;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HealthClient {

    private static final Logger log = LoggerFactory.getLogger(HealthClient.class);

    private final RestClient restClient;

    public HealthClient(RestClient sondeoRestClient) {
        this.restClient = sondeoRestClient;
    }

    public ResultadoSondeo sondear(String destino, int puerto, String url) {
        long inicio = System.nanoTime();
        try {
            ResponseEntity<String> r = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> { })
                    .toEntity(String.class);
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            int code = r.getStatusCode().value();
            EstadoComponente estado = interpretar(code, r.getBody());
            return new ResultadoSondeo(destino, puerto, url, estado, ms, code, OffsetDateTime.now());
        } catch (Exception e) {
            log.debug("Destino inaccesible o timeout en {} ({}): {}", destino, url, e.getMessage());
            return new ResultadoSondeo(destino, puerto, url,
                    EstadoComponente.DOWN, null, null, OffsetDateTime.now());
        }
    }

    private EstadoComponente interpretar(int code, String body) {
        // Diferenciar explicitamente errores de seguridad (401/403) de servidor caido
        if (code == 401 || code == 403) {
            return EstadoComponente.ERROR_ACCESO;
        }
        if (code >= 200 && code < 300) {
            boolean up = body != null && (body.contains("\"UP\"") || body.contains("UP"));
            return up ? EstadoComponente.UP : EstadoComponente.DOWN;
        }
        return EstadoComponente.DOWN;
    }
}
