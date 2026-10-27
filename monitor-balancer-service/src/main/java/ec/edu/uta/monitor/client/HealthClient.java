package ec.edu.uta.monitor.client;

import ec.edu.uta.monitor.dto.EstadoComponente;
import ec.edu.uta.monitor.dto.ResultadoSondeo;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HealthClient {

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
            return new ResultadoSondeo(destino, puerto, url,
                    interpretar(code, r.getBody()), ms, code, OffsetDateTime.now());
        } catch (Exception e) {
            return new ResultadoSondeo(destino, puerto, url,
                    EstadoComponente.DOWN, null, null, OffsetDateTime.now());
        }
    }

    private EstadoComponente interpretar(int code, String body) {
        if (code == 401 || code == 403) {
            return EstadoComponente.ERROR_ACCESO;
        }
        if (code >= 200 && code < 300) {
            boolean up = body != null && body.contains("\"UP\"");
            return up ? EstadoComponente.UP : EstadoComponente.DOWN;
        }
        return EstadoComponente.DOWN;
    }
}