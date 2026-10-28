package ec.edu.uta.monitor.service;

import ec.edu.uta.monitor.client.HealthClient;
import ec.edu.uta.monitor.config.MonitorProperties;
import ec.edu.uta.monitor.dto.EstadoComponente;
import ec.edu.uta.monitor.dto.EstadoGeneral;
import ec.edu.uta.monitor.dto.EstadoResponse;
import ec.edu.uta.monitor.dto.InstanciaResponse;
import ec.edu.uta.monitor.dto.InstanciaResumen;
import ec.edu.uta.monitor.dto.ResultadoSondeo;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class MonitorService {

    private static final int PUERTO_NGINX = 8090;

    private final HealthClient client;
    private final MonitorProperties props;
    private final AlertaService alertas;

    private final Map<String, ResultadoSondeo> ultimos = new ConcurrentHashMap<>();
    private EstadoGeneral generalAnterior;

    public MonitorService(HealthClient client, MonitorProperties props, AlertaService alertas) {
        this.client = client;
        this.props = props;
        this.alertas = alertas;
    }

    public synchronized void verificarTodo() {
        if (props.nginx() != null) {
            actualizar(client.sondear(props.nginx().nombre(), PUERTO_NGINX, props.nginx().url()));
        }
        if (props.instancias() != null) {
            for (MonitorProperties.Instancia i : props.instancias()) {
                actualizar(client.sondear(i.nombre(), i.puerto(), i.url()));
            }
        }
        EstadoGeneral actual = calcularGeneral();
        if (generalAnterior != null && generalAnterior != actual) {
            alertas.registrar("GLOBAL", generalAnterior.name(), actual.name(),
                    "El estado general cambio de " + generalAnterior + " a " + actual);
        }
        generalAnterior = actual;
    }

    private void actualizar(ResultadoSondeo nuevo) {
        if (nuevo == null) {
            return;
        }
        ResultadoSondeo previo = ultimos.put(nuevo.destino(), nuevo);
        if (previo != null && previo.estado() != nuevo.estado()) {
            alertas.registrar(nuevo.destino(), previo.estado().name(), nuevo.estado().name(),
                    nuevo.destino() + " paso de " + previo.estado() + " a " + nuevo.estado());
        }
    }

    private void asegurarDatos() {
        if (ultimos.isEmpty()) {
            verificarTodo();
        }
    }

    private ResultadoSondeo nginx() {
        return (props.nginx() != null) ? ultimos.get(props.nginx().nombre()) : null;
    }

    private List<ResultadoSondeo> backends() {
        List<ResultadoSondeo> lista = new ArrayList<>();
        if (props.instancias() != null) {
            for (MonitorProperties.Instancia i : props.instancias()) {
                ResultadoSondeo r = ultimos.get(i.nombre());
                if (r != null) {
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    public EstadoGeneral calcularGeneral() {
        ResultadoSondeo ng = nginx();
        if (ng == null || ng.estado() != EstadoComponente.UP) {
            return EstadoGeneral.NGINX_DOWN;
        }
        List<ResultadoSondeo> bs = backends();
        long arriba = bs.stream()
                .filter(b -> b.estado() == EstadoComponente.UP)
                .count();
        if (!bs.isEmpty() && arriba == bs.size()) {
            return EstadoGeneral.NORMAL;
        }
        return arriba == 0 ? EstadoGeneral.SIN_BACKENDS : EstadoGeneral.DEGRADADO;
    }

    public EstadoResponse estado() {
        asegurarDatos();
        List<InstanciaResumen> resumen = backends().stream()
                .map(b -> new InstanciaResumen(b.puerto(), b.estado(), b.latenciaMs()))
                .toList();
        OffsetDateTime revision = ultimos.values().stream()
                .map(ResultadoSondeo::revision)
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());
        String estadoNginx = (nginx() != null && nginx().estado() != null)
                ? nginx().estado().name() : EstadoComponente.DOWN.name();
        return new EstadoResponse(estadoNginx, calcularGeneral(), resumen, revision);
    }

    public List<InstanciaResponse> instancias() {
        asegurarDatos();
        return backends().stream()
                .map(b -> new InstanciaResponse(
                        b.puerto(), b.url(), b.estado(), b.latenciaMs(), b.revision()))
                .toList();
    }
}
