package ec.edu.uta.monitor.service;

import ec.edu.uta.monitor.client.HealthClient;
import ec.edu.uta.monitor.config.MonitorProperties;
import ec.edu.uta.monitor.dto.EstadoComponente;
import ec.edu.uta.monitor.dto.EstadoGeneral;
import ec.edu.uta.monitor.dto.EstadoResponse;
import ec.edu.uta.monitor.dto.InstanciaResumen;
import ec.edu.uta.monitor.dto.ResultadoSondeo;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MonitorService {

    private static final int PUERTO_NGINX = 8090;

    private final HealthClient client;
    private final MonitorProperties props;

    public MonitorService(HealthClient client, MonitorProperties props) {
        this.client = client;
        this.props = props;
    }

    public EstadoResponse estado() {
        ResultadoSondeo nginx = client.sondear(
                props.nginx().nombre(), PUERTO_NGINX, props.nginx().url());

        List<ResultadoSondeo> backends = props.instancias().stream()
                .map(i -> client.sondear(i.nombre(), i.puerto(), i.url()))
                .toList();

        long arriba = backends.stream()
                .filter(b -> b.estado() == EstadoComponente.UP)
                .count();

        EstadoGeneral general;
        if (nginx.estado() != EstadoComponente.UP) {
            general = EstadoGeneral.NGINX_DOWN;
        } else if (arriba == backends.size()) {
            general = EstadoGeneral.NORMAL;
        } else if (arriba == 0) {
            general = EstadoGeneral.SIN_BACKENDS;
        } else {
            general = EstadoGeneral.DEGRADADO;
        }

        List<InstanciaResumen> resumen = backends.stream()
                .map(b -> new InstanciaResumen(b.puerto(), b.estado(), b.latenciaMs()))
                .toList();

        return new EstadoResponse(nginx.estado().name(), general, resumen, OffsetDateTime.now());
    }
}