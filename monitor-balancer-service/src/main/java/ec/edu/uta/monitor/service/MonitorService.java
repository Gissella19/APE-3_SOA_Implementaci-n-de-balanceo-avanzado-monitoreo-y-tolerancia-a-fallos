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
        actualizar(client.sondear(props.nginx().nombre(), PUERTO_NGINX, props.nginx().url()));
        for (MonitorProperties.Instancia i : props.instancias()) {
            actualizar(client.sondear(i.nombre(), i.puerto(), i.url()));
        }
        EstadoGeneral actual = calcularGeneral();
        if (generalAnterior != null && generalAnterior != actual) {
            alertas.registrar("GLOBAL", generalAnterior.name(), actual.name(),
                    "El estado general cambio de " + generalAnterior + " a " + actual);
        }
        generalAnterior = actual;
    }

    private void actualizar(ResultadoSondeo nuevo) {
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
        return ultimos.get(props.nginx().nombre());
    }

    private List<ResultadoSondeo> backends() {
        List<ResultadoSondeo> lista = new ArrayList<>();
        for (MonitorProperties.Instancia i : props.instancias()) {
            lista.add(ultimos.get(i.nombre()));
        }
        return lista;
    }

    private EstadoGeneral calcularGeneral() {
        if (nginx().estado() != EstadoComponente.UP) {
            return EstadoGeneral.NGINX_DOWN;
        }
        long arriba = backends().stream()
                .filter(b -> b.estado() == EstadoComponente.UP)
                .count();
        if (arriba == backends().size()) {
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
                .max(OffsetDateTime::compareTo)
                .orElse(OffsetDateTime.now());
        return new EstadoResponse(nginx().estado().name(), calcularGeneral(), resumen, revision);
    }

    public List<InstanciaResponse> instancias() {
        asegurarDatos();
        return backends().stream()
                .map(b -> new InstanciaResponse(
                        b.puerto(), b.url(), b.estado(), b.latenciaMs(), b.revision()))
                .toList();
    }
}